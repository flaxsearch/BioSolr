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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.search.Query;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrIndexSearcher;
import org.junit.Test;

public class TestXJoinQParserPlugin extends AbstractXJoinTestCase {

  static final String componentName = "xjoin";
  static final String parserName = "xjoin";

  @Test
  public void test() throws Exception {
    SolrCore core = h.getCore();

    XJoinSearchComponent xjsc = (XJoinSearchComponent)core.getSearchComponent(componentName);
    SimpleXJoinResultsFactory xjrf = (SimpleXJoinResultsFactory)xjsc.getResultsFactory();
    XJoinResults<?> results = xjrf.getResults(null);
    
    // mock SolrQueryRequest with join results in the context
    SolrQueryRequest req = mock(SolrQueryRequest.class);
    Map<Object, Object> context = new HashMap<>();
    context.put(xjsc.getResultsTag(), results);
    when(req.getContext()).thenReturn(context);
    when(req.getCore()).thenReturn(core);
    when(req.getSchema()).thenReturn(core.getLatestSchema());
    
    ModifiableSolrParams localParams = new ModifiableSolrParams();
    localParams.add(QueryParsing.V, componentName);
    
    QParserPlugin qpp = core.getQueryPlugin(parserName);
    QParser qp = qpp.createParser(null, localParams, null, req);
    Query q = qp.parse();
    
    SolrIndexSearcher searcher = core.getRegisteredSearcher().get();
    DocSet docs = searcher.getDocSet(q);
    searcher.close();
    
    assertEquals(2, docs.size());
    DocIterator it = docs.iterator();
    assertTrue(it.hasNext());
    assertEquals(1, it.nextDoc());
    assertTrue(it.hasNext());
    assertEquals(3, it.nextDoc());
    assertFalse(it.hasNext());
  }
  
}
