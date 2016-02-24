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

import java.io.IOException;
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
import org.apache.solr.search.SyntaxError;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestXJoinQParserPlugin extends AbstractXJoinTestCase {

  static final String COMPONENT_NAME_0 = "xjoin0";
  static final String COMPONENT_NAME = "xjoin";
  static final String COMPONENT_NAME_2 = "xjoin2";
  static final String COMPONENT_NAME_3 = "xjoin3";
  static final String COMPONENT_NAME_4 = "xjoin4";
  static final String PARSER_NAME = "xjoin";
  
  static SolrCore core;
  static SolrQueryRequest req;
  static SolrIndexSearcher searcher;

  private static void initComponent(SolrCore core, Map<Object, Object> context, String componentName) throws IOException {
    XJoinSearchComponent xjsc = (XJoinSearchComponent)core.getSearchComponent(componentName);
    DummyXJoinResultsFactory xjrf = (DummyXJoinResultsFactory)xjsc.getResultsFactory();
    XJoinResults<?> results = xjrf.getResults(null);
    context.put(xjsc.getResultsTag(), results);    
  }
  
  private static Query parse(String v) throws SyntaxError {
    ModifiableSolrParams localParams = new ModifiableSolrParams();
    localParams.add(QueryParsing.V, v);
    QParserPlugin qpp = core.getQueryPlugin(PARSER_NAME);
    QParser qp = qpp.createParser(null, localParams, null, req);
    return qp.parse();
  }
  
  @BeforeClass
  public static void initialise() throws Exception {
    core = h.getCore();

    // set up mock SOLR query request
    req = mock(SolrQueryRequest.class);
    Map<Object, Object> context = new HashMap<>();
    when(req.getContext()).thenReturn(context);
    when(req.getCore()).thenReturn(core);
    when(req.getSchema()).thenReturn(core.getLatestSchema());

    // put results for XJoin components in request context
    initComponent(core, context, COMPONENT_NAME_0);
    initComponent(core, context, COMPONENT_NAME);
    initComponent(core, context, COMPONENT_NAME_2);
    initComponent(core, context, COMPONENT_NAME_3);
    initComponent(core, context, COMPONENT_NAME_4);
    
    // get a search, used by some tests
    searcher = core.getRegisteredSearcher().get();
  }
  
  @AfterClass
  public static void destroy() throws Exception {
    searcher.close();
  }
  
  @Test
  public void testSingleComponent() throws Exception {
    Query q = parse(COMPONENT_NAME);
    DocSet docs = searcher.getDocSet(q);

    assertEquals(2, docs.size());
    DocIterator it = docs.iterator();
    assertTrue(it.hasNext());
    assertEquals(1, it.nextDoc());
    assertTrue(it.hasNext());
    assertEquals(3, it.nextDoc());
    assertFalse(it.hasNext());
  }
  
  @Test
  public void testBooleanCombination() throws Exception {
    Query q = parse(COMPONENT_NAME + " AND " + COMPONENT_NAME_2);
    DocSet docs = searcher.getDocSet(q);

    assertEquals(1, docs.size());
    DocIterator it = docs.iterator();
    assertTrue(it.hasNext());
    assertEquals(3, it.nextDoc());
    assertFalse(it.hasNext());    
  }
  
  @Test(expected=XJoinQParserPlugin.Exception.class)
  public void testConflictingJoinFields() throws Exception {
    parse(COMPONENT_NAME + " OR " + COMPONENT_NAME_3);
  }
  
  @Test
  public void testNoResults() throws Exception {
    Query q = parse(COMPONENT_NAME_0);
    DocSet docs = searcher.getDocSet(q);
    assertEquals(0, docs.size());
  }
  
  @Test
  public void testMultiValued() throws Exception {
    Query q = parse(COMPONENT_NAME_4);
    DocSet docs = searcher.getDocSet(q);

    assertEquals(4, docs.size());
    DocIterator it = docs.iterator();
    assertTrue(it.hasNext());
    assertEquals(0, it.nextDoc());
    assertTrue(it.hasNext());
    assertEquals(1, it.nextDoc());
    assertTrue(it.hasNext());
    assertEquals(2, it.nextDoc());
    assertTrue(it.hasNext());
    assertEquals(3, it.nextDoc());
    assertFalse(it.hasNext());    
  }
  
}
