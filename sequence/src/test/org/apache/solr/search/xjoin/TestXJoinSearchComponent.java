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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.QParserPlugin;
import org.junit.Test;

public class TestXJoinSearchComponent extends AbstractXJoinTestCase {

  static String componentName = "xjoin";
  static String requestHandler = "standard";

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void test() {
    SolrCore core = h.getCore();

    SearchComponent sc = core.getSearchComponent(componentName);
    assertTrue("XJoinSearchComponent not found in solrconfig", sc != null);
      
    QParserPlugin qp = core.getQueryPlugin("xjoin");
    assertTrue("XJoinQParserPlugin not found in solrconfig", qp != null);
    
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.add("q", "*:*");
    params.add("fq", "{!xjoin}xjoin");

    SolrQueryResponse rsp = new SolrQueryResponse();
    rsp.add("responseHeader", new SimpleOrderedMap<>());
    SolrQueryRequest req = new LocalSolrQueryRequest(core, params);

    SolrRequestHandler handler = core.getRequestHandler(requestHandler);
    handler.handleRequest(req, rsp);
    req.close();
      
    NamedList results = rsp.getValues();
    NamedList xjoin = (NamedList)results.get(componentName);
    assertTrue(componentName + " should not be null", xjoin != null);
    assertEquals("a test string", xjoin.get("string"));
    
    String[] values = { "1", "3", "8" };
    Set<String> expected = new HashSet<>(Arrays.asList(values));
    Set<String> actual = new HashSet<String>((List<String>)xjoin.get("join_ids"));
    assertEquals(expected, actual);
    
    DocList docs = ((ResultContext)results.get("response")).docs;
    assertEquals(2, docs.size());
    DocIterator it = docs.iterator();
    assertTrue(it.hasNext());
    assertEquals(1, it.nextDoc());
    assertTrue(it.hasNext());
    assertEquals(3, it.nextDoc());
    assertFalse(it.hasNext());
  }
  
}
