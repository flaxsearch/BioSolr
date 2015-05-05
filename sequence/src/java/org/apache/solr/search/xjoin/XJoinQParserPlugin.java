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

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.iterators.TransformIterator;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermsFilter;
import org.apache.lucene.search.AutomatonQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FieldCacheTermsFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.MultiTermQueryWrapperFilter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
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
 * QParserPlugin for extracting join ids from the results stored in an XJoin search
 * component.
 */
public class XJoinQParserPlugin extends QParserPlugin {
  
  public static final String NAME = "xjoin";

  /** The separator to use in the underlying suggester */
  public static final String SEPARATOR = "separator";

  /** Choose the internal algorithm */
  private static final String METHOD = "method";
  
  @Override @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
    // nothing to do
  }
 
  private static enum Method {
    termsFilter {
      @Override
      Filter makeFilter(String fname, Iterator<BytesRef> it) {
        return new TermsFilter(fname, IteratorUtils.toList(it));
      }
    },
    booleanQuery {
      @Override
      Filter makeFilter(String fname, Iterator<BytesRef> it) {
        BooleanQuery bq = new BooleanQuery(true);
        while (it.hasNext()) {
          bq.add(new TermQuery(new Term(fname, it.next())), BooleanClause.Occur.SHOULD);
        }
        return new QueryWrapperFilter(bq);
      }
    },
    automaton {
      @Override
      Filter makeFilter(String fname, Iterator<BytesRef> it) {
        Automaton union = Automata.makeStringUnion(IteratorUtils.toList(it));
        return new MultiTermQueryWrapperFilter<AutomatonQuery>(new AutomatonQuery(new Term(fname), union)) {
        };
      }
    },
    docValuesTermsFilter {//on 4x this is FieldCacheTermsFilter but we use the 5x name any way
      //note: limited to one val per doc
      @Override
      Filter makeFilter(String fname, Iterator<BytesRef> it) {
        return new FieldCacheTermsFilter(fname, IteratorUtils.toArray(it, BytesRef.class));
      }
    };

    //abstract Filter makeFilter(String fname, BytesRef... byteRefs);
    abstract Filter makeFilter(String fname, Iterator<BytesRef> it);
  }
  
  // transformer between Object and BytesRef
  static private Transformer<Object, BytesRef> transformer(final FieldType ft) {
    return new Transformer<Object, BytesRef>() {
      
      BytesRef term = new BytesRef();
      
      @Override
      public BytesRef transform(Object joinId) {
        String joinStr = joinId.toString();
        // logic same as TermQParserPlugin
        if (ft != null) {
          ft.readableToIndexed(joinStr, term);
        } else {
          term.copyChars(joinStr);
        }
        return BytesRef.deepCopyOf(term);
      }
      
    };
  }

  /**
   * Like fq={!xjoin}xjoin_component_name OR xjoin_component_name2
   */
  @Override
  public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
    return new XJoinQParser(qstr, localParams, params, req);
  }
  
  static class XJoinQParser extends QParser implements JoinSpec.Iterable {
    
    private String joinField;

    public XJoinQParser(String qstr, SolrParams localParams, SolrParams params,SolrQueryRequest req) {
      super(qstr, localParams, params, req);
      joinField = null;
    }

    @Override
    public Query parse() throws SyntaxError {
      Method method = Method.valueOf(localParams.get(METHOD, Method.termsFilter.name()));
      JoinSpec<?> js = JoinSpec.parse(localParams.get(QueryParsing.V));
      Iterator<?> it = js.iterator(this);
      if (joinField == null) {
        throw new RuntimeException("No XJoin component in query");
      }
      FieldType ft = req.getSchema().getFieldTypeNoEx(joinField);
      Iterator<BytesRef> bytesRefs = new TransformIterator<Object, BytesRef>(it, transformer(ft));
      return new SolrConstantScoreQuery(method.makeFilter(joinField, bytesRefs));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> Iterator<T> iterator(String componentName) {
      XJoinSearchComponent xJoin = (XJoinSearchComponent)req.getCore().getSearchComponent(componentName);
      if (joinField == null) {
        joinField = xJoin.getJoinField();
      } else {
        if (! xJoin.getJoinField().equals(joinField)) {
          throw new RuntimeException("XJoin components used in the same query must have same join field");
        }
      }
      XJoinResults<T> results = (XJoinResults<T>)req.getContext().get(xJoin.getResultsTag());
      if (results == null) {
        throw new RuntimeException("No xjoin results in request context");
      }
      return results.getJoinIds().iterator();
    }
    
  }
  
}
