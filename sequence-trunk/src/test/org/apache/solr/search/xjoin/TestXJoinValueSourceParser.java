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

import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SolrIndexSearcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestXJoinValueSourceParser extends AbstractXJoinTestCase {

  static double value = 0.5;
  static double defaultValue = 1.0;
  static String componentName = "xjoin";
  static String resultAttribute = "score";
  
  static SolrIndexSearcher searcher;
  static SolrQueryRequest sqr;
  static int missingDoc;

  @BeforeClass
  public static void initialise() throws Exception {
    SolrCore core = h.getCore();

    XJoinSearchComponent xjsc = (XJoinSearchComponent)core.getSearchComponent(componentName);
    DummyXJoinResultsFactory xjrf = (DummyXJoinResultsFactory)xjsc.getResultsFactory();
    XJoinResults<?> results = xjrf.getResults(null);
    
    // mock SolrQueryRequest with join results in the context
    sqr = mock(SolrQueryRequest.class);
    Map<Object, Object> context = new HashMap<>();
    context.put(xjsc.getResultsTag(), results);
    when(sqr.getContext()).thenReturn(context);
    when(sqr.getCore()).thenReturn(core);
    
    searcher = core.getRegisteredSearcher().get();
    
    missingDoc = new Integer(xjrf.getMissingId());
  }
  
  @AfterClass
  public static void tidyUp() throws Exception {
    searcher.close();
  }
  
  // mock function qparser returning a given argument
  // (to match getScore() in the xjoin results)
  private FunctionQParser mockFunctionQParser(String arg) throws Exception {
    FunctionQParser fqp = mock(FunctionQParser.class);
    when(fqp.getReq()).thenReturn(sqr);    
    when(fqp.parseArg()).thenReturn(arg);
    return fqp;
  }
  
  @SuppressWarnings({ "rawtypes" })
  private FunctionValues functionValues(NamedList initArgs, String arg) throws Exception {
    FunctionQParser fqp = mockFunctionQParser(arg);
    XJoinValueSourceParser vsp = new XJoinValueSourceParser();
    vsp.init(initArgs);
    ValueSource vs = vsp.parse(fqp);
    return vs.getValues(null, searcher.getLeafReader().getContext());
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testAttributeArg() throws Exception {
    NamedList initArgs = new NamedList();
    initArgs.add(XJoinParameters.INIT_XJOIN_COMPONENT_NAME, componentName);
    FunctionValues fv = functionValues(initArgs, resultAttribute);
    assertEquals(value, fv.doubleVal(0), 0);
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testComponentArg() throws Exception {
    NamedList initArgs = new NamedList();
    initArgs.add(XJoinParameters.INIT_ATTRIBUTE, resultAttribute);
    FunctionValues fv = functionValues(initArgs, componentName);
    assertEquals(value, fv.doubleVal(0), 0);
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testDefault() throws Exception {
    NamedList initArgs = new NamedList();
    initArgs.add(XJoinParameters.INIT_ATTRIBUTE, resultAttribute);
    initArgs.add(XJoinParameters.INIT_DEFAULT_VALUE, defaultValue);
    FunctionValues fv = functionValues(initArgs, componentName);
    assertEquals(defaultValue, fv.doubleVal(missingDoc), 0);
  }
  
}
