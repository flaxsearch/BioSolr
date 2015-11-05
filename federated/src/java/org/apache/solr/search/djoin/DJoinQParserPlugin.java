package org.apache.solr.search.djoin;

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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.Weight;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.MergeStrategy;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.RankQuery;
import org.apache.solr.search.SolrIndexSearcher.QueryCommand;
import org.apache.solr.search.SyntaxError;

public class DJoinQParserPlugin extends QParserPlugin {

  @Override @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
  }

  @Override
  public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
    return new QParser(qstr, localParams, params, req) {

      @Override
      public Query parse() throws SyntaxError {
        return new RankQuery() {

          private Query mainQuery;
          
          @Override @SuppressWarnings("rawtypes")
          public TopDocsCollector getTopDocsCollector(int len, QueryCommand cmd, IndexSearcher searcher) throws IOException {
            Sort sort = cmd.getSort();
            if (sort == null) {
              return TopScoreDocCollector.create(len, false);
            } else {
              return TopFieldCollector.create(sort.rewrite(searcher), len, false, true, true, false);
            }
          }

          @Override
          public MergeStrategy getMergeStrategy() {
            return new DJoinMergeStrategy();
          }

          @Override
          public RankQuery wrap(Query mainQuery) {
            this.mainQuery = mainQuery;
            return this;
          }

          @Override
          public Query rewrite(IndexReader reader) throws IOException {
            return mainQuery.rewrite(reader);
          }
          
          @Override
          public Weight createWeight(IndexSearcher searcher) throws IOException {
            return mainQuery.createWeight(searcher);
          }
          
        };
      }
      
    };
  }

}
