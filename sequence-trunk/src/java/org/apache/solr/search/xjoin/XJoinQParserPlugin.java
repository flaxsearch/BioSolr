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

import java.util.Iterator;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermsQuery;
import org.apache.lucene.search.AutomatonQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocValuesTermsQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.FieldType;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrConstantScoreQuery;
import org.apache.solr.search.SyntaxError;

/**
 * QParserPlugin for extracting join ids from the results stored in XJoin search
 * components.
 */
public class XJoinQParserPlugin extends QParserPlugin {
  
  public static final String NAME = "xjoin";

  /** For choosing the internal algorithm */
  private static final String METHOD = "method";
  
  @Override @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
    // nothing to do
  }
 
  // this code is modified from TermsQParserPlugin
  private static enum Method {
    termsFilter {
      @Override
      Filter makeFilter(String fname, Iterator<BytesRef> it) {
      	BytesRef[] bytesRefs = (BytesRef[])IteratorUtils.toArray(it, BytesRef.class);
        return new QueryWrapperFilter(new TermsQuery(fname, bytesRefs));
      }
    },
    booleanQuery {
      @Override
      Filter makeFilter(String fname, Iterator<BytesRef> it) {
        BooleanQuery.Builder bq = new BooleanQuery.Builder();
        bq.setDisableCoord(true);
        while (it.hasNext()) {
          bq.add(new TermQuery(new Term(fname, it.next())), BooleanClause.Occur.SHOULD);
        }
        return new QueryWrapperFilter(bq.build());
      }
    },
    automaton {
      @Override
      @SuppressWarnings("unchecked")
      Filter makeFilter(String fname, Iterator<BytesRef> it) {
        Automaton union = Automata.makeStringUnion(IteratorUtils.toList(it));
        return new QueryWrapperFilter(new AutomatonQuery(new Term(fname), union));
      }
    },
    docValuesTermsFilter {
      @Override
      Filter makeFilter(String fname, Iterator<BytesRef> it) {
    	BytesRef[] bytesRefs = (BytesRef[])IteratorUtils.toArray(it, BytesRef.class);
        return new QueryWrapperFilter(new DocValuesTermsQuery(fname, bytesRefs));
      }
    };

    //abstract Filter makeFilter(String fname, BytesRef... byteRefs);
    abstract Filter makeFilter(String fname, Iterator<BytesRef> it);
  }
  
  // transformer from Object to BytesRef (using the given FieldType)
  static private Transformer transformer(final FieldType ft) {
    return new Transformer() {
      
      BytesRefBuilder term = new BytesRefBuilder();
      
      @Override
      public BytesRef transform(Object joinId) {
        String joinStr = joinId.toString();
        // logic same as TermQParserPlugin
        if (ft != null) {
          ft.readableToIndexed(joinStr, term);
        } else {
          term.copyChars(joinStr);
        }
        return term.toBytesRef();
      }
      
    };
  }

  /**
   * Like fq={!xjoin}xjoin_component_name OR xjoin_component_name2
   */
  @Override
  @SuppressWarnings("rawtypes")
  public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
    return new XJoinQParser(qstr, localParams, params, req);
  }
  
  static class XJoinQParser<T extends Comparable<T>> extends QParser implements JoinSpec.Iterable {
    
    // record the join field when retrieving external results
    // must be the same for all external sources referenced in our query
    private String joinField;

    public XJoinQParser(String qstr, SolrParams localParams, SolrParams params,SolrQueryRequest req) {
      super(qstr, localParams, params, req);
      joinField = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Query parse() throws SyntaxError {
      Method method = Method.valueOf(localParams.get(METHOD, Method.termsFilter.name()));
      JoinSpec<T> js = JoinSpec.parse(localParams.get(QueryParsing.V));
      Iterator<T> it = js.iterator(this);
      if (joinField == null) {
        throw new Exception("No XJoin component referenced by query");
      }
      FieldType ft = req.getSchema().getFieldTypeNoEx(joinField);
      Iterator<BytesRef> bytesRefs = new TransformIterator(it, transformer(ft));
      if (! bytesRefs.hasNext()) {
        return new BooleanQuery.Builder().build(); // matches nothing
      }
      return new SolrConstantScoreQuery(method.makeFilter(joinField, bytesRefs));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Iterator<T> iterator(String componentName) {
      XJoinSearchComponent xJoin = (XJoinSearchComponent)req.getCore().getSearchComponent(componentName);
      if (joinField == null) {
        joinField = xJoin.getJoinField();
      } else if (! xJoin.getJoinField().equals(joinField)) {
        throw new Exception("XJoin components used in the same query must have same join field");
      }
      XJoinResults<T> results = (XJoinResults<T>)req.getContext().get(xJoin.getResultsTag());
      if (results == null) {
        throw new Exception("No xjoin results in request context");
      }
      return results.getJoinIds().iterator();
    }
    
  }
  
  @SuppressWarnings("serial")
  static class Exception extends RuntimeException {
    public Exception(String message) {
      super(message);
    }
  }
  
}
