package org.apache.solr.search.xjoin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.Grouping;

/**
 * SOLR Search Component for performing an "x-join". It must be added to a request handler
 * in both the first and last component lists.
 * 
 * In prepare(), it obtains external process results (based on parameters in the SOLR query
 * URL) and places them into the request context.
 * 
 * In process(), it appends (selectable) attributes of the external process results to the
 * query results.
 * 
 * Note that results can be sorted or boosted by a property of external results by using
 * the associated XjoinValueSourceParser (creating a custom function which may be referenced
 * in, for example, a sort spec or a boost query).
 */
public class XJoinSearchComponent extends SearchComponent {

  // factory for creating XJoinResult objects per search
  private XJoinResultsFactory<?> factory;

  // document field on which to join with external results
  private String joinField;
  
  /**
   * Initialise the component by instantiating our factory class, and initialising
   * the join field.
   */
  @Override
  @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
    super.init(args);
    
    try {
      Class<?> factoryClass = Class.forName((String)args.get(XJoinParameters.INIT_RESULTS_FACTORY));
      factory = (XJoinResultsFactory<?>)factoryClass.newInstance();
      factory.init((NamedList)args.get(XJoinParameters.EXTERNAL_PREFIX));
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    
    joinField = (String)args.get(XJoinParameters.INIT_JOIN_FIELD);
  }
  
  // get the results factory
  /*package*/ XJoinResultsFactory<?> getResultsFactory() {
    return factory;
  }
  
  // get the context tag for XJoin results
  /*package*/ String getResultsTag() {
    return XJoinResults.class.getName() + "::" + getName();
  }
  
  /**
   * Generate external process results (if they have not already been generated).
   */
  @Override
  public void prepare(ResponseBuilder rb) throws IOException {
    SolrParams params = rb.req.getParams();
    if (! params.getBool(getName(), false)) {
      return;
    }
      
    XJoinResults<?> results = (XJoinResults<?>)rb.req.getContext().get(getResultsTag());
    if (results != null) {
      return;
    }
      
    // generate external process results, by passing 'external' prefixed parameters
    // from the query string to our factory
    String prefix = getName() + "." + XJoinParameters.EXTERNAL_PREFIX + ".";
    ModifiableSolrParams externalParams = new ModifiableSolrParams();
    for (Iterator<String> it = params.getParameterNamesIterator(); it.hasNext(); ) {
      String name = it.next();
      if (name.startsWith(prefix)) {
        externalParams.set(name.substring(prefix.length()), params.get(name));
      }
    }
    results = factory.getResults(externalParams);
    rb.req.getContext().put(getResultsTag(), results);
  }

  /**
   * Match up search results and add corresponding data for each result (if we have query
   * results available).
   */
  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void process(ResponseBuilder rb) throws IOException {
    SolrParams params = rb.req.getParams();
    if (! params.getBool(getName(), false)) {
      return;
    }

    XJoinResults<?> results = (XJoinResults<?>)rb.req.getContext().get(getResultsTag());
    if (results == null || rb.getResults() == null) {
      return;
    }

    // general results
    FieldAppender appender = new FieldAppender((String)params.get(getName() + "." + XJoinParameters.RESULTS_FIELD_LIST, "*"));
    NamedList general = appender.addNamedList(rb.rsp.getValues(), getName(), results);

    // per join id results
    FieldAppender docAppender = new FieldAppender((String)params.get(getName() + "." + XJoinParameters.DOC_FIELD_LIST, "*"));    
    Set<String> joinFields = new HashSet<>();
    joinFields.add(joinField);
    
    List<String> joinIds = new ArrayList<>();
    for (Iterator<Integer> it = docIterator(rb); it.hasNext(); ) {
      Document doc = rb.req.getSearcher().doc(it.next(), joinFields);
      for (String joinId : doc.getValues(joinField)) {
        if (! joinIds.contains(joinId)) {
          joinIds.add(joinId);
        }
      }
    }
    
    List externalList = new ArrayList();
    general.add("external", externalList);
    
    for (String joinId : joinIds) {
      Object object = results.getResult(joinId);
      if (object == null) continue;
      SimpleOrderedMap external = new SimpleOrderedMap<>();
      externalList.add(external);
      external.add("joinId", joinId);
      if (object instanceof Iterable) {
        for (Object item : (Iterable)object) {
          docAppender.addNamedList(external, "doc", item);
        }
      } else {
        docAppender.addNamedList(external, "doc", object);        
      }
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private Iterator<Integer> docIterator(ResponseBuilder rb) {
    if (rb.grouping()) {
      List<Integer> docList = new ArrayList<>();
      NamedList values = rb.rsp.getValues();
      NamedList grouped = (NamedList)values.get("grouped");
      for (String field : rb.getGroupingSpec().getFields()) {
        NamedList fieldResults = (NamedList)grouped.get(field);
        if (rb.getGroupingSpec().getResponseFormat() == Grouping.Format.grouped) {
          List<NamedList> groups = (List<NamedList>)fieldResults.get("groups");
          for (NamedList group : groups) {
            for (DocIterator it = ((DocList)group.get("doclist")).iterator(); it.hasNext(); ) {
              docList.add(it.nextDoc());
            }
          }
        } else {
          for (DocIterator it = ((DocList)fieldResults.get("doclist")).iterator(); it.hasNext(); ) {
            docList.add(it.nextDoc());
          }
        }
      }
      return docList.iterator();
    } else {
      return rb.getResults().docList.iterator();
    }
    
  }
  
  /*package*/ String getJoinField() {
    return joinField;
  }

  @Override
  public String getDescription() {
    return "$description$";
  }

//  @Override
//  public String getSource() {
//    return "$source$";
//  }

}
