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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
      Filter makeFilter(String fname, BytesRef... bytesRefs) {
        return new TermsFilter(fname, bytesRefs);
      }
    },
    booleanQuery {
      @Override
      Filter makeFilter(String fname, BytesRef... byteRefs) {
        BooleanQuery bq = new BooleanQuery(true);
        for (BytesRef byteRef : byteRefs) {
          bq.add(new TermQuery(new Term(fname, byteRef)), BooleanClause.Occur.SHOULD);
        }
        return new QueryWrapperFilter(bq);
      }
    },
    automaton {
      @Override
      Filter makeFilter(String fname, BytesRef... byteRefs) {
        Automaton union = Automata.makeStringUnion(Arrays.asList(byteRefs));
        return new MultiTermQueryWrapperFilter<AutomatonQuery>(new AutomatonQuery(new Term(fname), union)) {
        };
      }
    },
    docValuesTermsFilter {//on 4x this is FieldCacheTermsFilter but we use the 5x name any way
      //note: limited to one val per doc
      @Override
      Filter makeFilter(String fname, BytesRef... byteRefs) {
        return new FieldCacheTermsFilter(fname, byteRefs);
      }
    };

    abstract Filter makeFilter(String fname, BytesRef... byteRefs);
  }

  /**
   * Like fq={!xjoin}xjoin_component_name
   */
  @Override
  public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
    return new QParser(qstr, localParams, params, req) {
      @Override
      public Query parse() throws SyntaxError {
        String componentName = localParams.get(QueryParsing.V);//never null
        XJoinSearchComponent xJoin = (XJoinSearchComponent)req.getCore().getSearchComponent(componentName);
        
        //TODO boolean combinations e.g. {!xjoin}x1 OR (x2 AND x3)

        FieldType ft = req.getSchema().getFieldTypeNoEx(xJoin.getJoinField());
        Method method = Method.valueOf(localParams.get(METHOD, Method.termsFilter.name()));
        //TODO pick the default method based on various heuristics from benchmarks

        XJoinResults<?> results = (XJoinResults<?>)req.getContext().get(xJoin.getResultsTag());
        if (results == null) {
          throw new RuntimeException("No xjoin results in request context");
        }

        List<Object> joinIds = new ArrayList<>();
        for (Object joinId : results.getJoinIds()) {
          joinIds.add(joinId);
        }

        BytesRef[] bytesRefs = new BytesRef[joinIds.size()];
        BytesRef term = new BytesRef();
        for (int i = 0; i < bytesRefs.length; i++) {
          // now we convert join ids to Strings
          String joinStr = joinIds.get(i).toString();
          // logic same as TermQParserPlugin
          if (ft != null) {
          ft.readableToIndexed(joinStr, term);
          } else {
            term.copyChars(joinStr);
          }
          bytesRefs[i] = BytesRef.deepCopyOf(term);
        }

        return new SolrConstantScoreQuery(method.makeFilter(xJoin.getJoinField(), bytesRefs));
      }
    };
  }
  
}
