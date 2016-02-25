package org.apache.solr.search.xjoin.simple;

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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.IteratorUtils;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.xjoin.XJoinResults;
import org.junit.Test;

import com.jayway.jsonpath.PathNotFoundException;

public class TestSimpleXJoinResultsFactory {

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testJson() throws IOException {
    NamedList args = new NamedList();
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_TYPE, SimpleXJoinResultsFactory.Type.JSON.toString());
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_ROOT_URL, getClass().getResource("results.json").toString());
    
    NamedList globalPaths = new NamedList();
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_GLOBAL_FIELD_PATHS, globalPaths);
    globalPaths.add("total", "$.count");
    
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_JOIN_ID_PATH, "$.hits[*].id");
    
    NamedList resultPaths = new NamedList();
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_RESULT_FIELD_PATHS, resultPaths);
    resultPaths.add("colour", "$.hits[?(@.id == 'JOINID')].colour");
    resultPaths.add("value", "$.meta[?(@.id == 'JOINID')].value");
    
    testResultsFile(args, true, true);
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testXml() throws IOException {
    NamedList args = new NamedList();
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_TYPE, SimpleXJoinResultsFactory.Type.XML.toString());
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_ROOT_URL, getClass().getResource("results.xml").toString());
    
    NamedList globalPaths = new NamedList();
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_GLOBAL_FIELD_PATHS, globalPaths);
    globalPaths.add("total", "/results/count");
    
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_JOIN_ID_PATH, "/results/hits/doc/@id");
    
    NamedList resultPaths = new NamedList();
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_RESULT_FIELD_PATHS, resultPaths);
    resultPaths.add("colour", "/results/hits/doc[@id='JOINID']/colour");
    resultPaths.add("value", "/results/meta/doc[@id='JOINID']/@value");
    
    testResultsFile(args, true, true);
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void testResultsFile(NamedList args, boolean globalValues, boolean resultValues) throws IOException {
    SimpleXJoinResultsFactory factory = new SimpleXJoinResultsFactory();
    factory.init(args);
    
    SolrParams params = new ModifiableSolrParams();
    XJoinResults<String> results = factory.getResults(params);
    
    Map<String, String> values = (Map<String, String>)results;
    if (globalValues) {
      assertEquals(312, values.get("total"));
    } else {
      assertEquals(0, values.size());
    }
    
    Set<String> joinIds = new HashSet<>(IteratorUtils.toList(results.getJoinIds().iterator()));
    assertEquals(new HashSet<>(Arrays.asList(new String[] { "a3e5bd", "252ae1", "912151" })), joinIds);
    Map<String, String> result1 = (Map<String, String>)results.getResult("a3e5bd");
    Map<String, String> result2 = (Map<String, String>)results.getResult("252ae1");
    if (resultValues) {
      assertEquals("blue", result1.get("colour"));
      assertEquals(10.5, result2.get("value"));
    } else {
      assertEquals(0, result1.size());
      assertEquals(0, result2.size());
    }
  }
  
  @Test(expected=PathNotFoundException.class)
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testNoJoinIdsAtPath() throws IOException {
    NamedList args = new NamedList();
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_TYPE, SimpleXJoinResultsFactory.Type.JSON.toString());
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_ROOT_URL, getClass().getResource("results.json").toString());
    
    NamedList globalPaths = new NamedList();
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_GLOBAL_FIELD_PATHS, globalPaths);
    globalPaths.add("total", "$.count");
    
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_JOIN_ID_PATH, "$.no.ids.at.this.path");
    
    SimpleXJoinResultsFactory factory = new SimpleXJoinResultsFactory();
    factory.init(args);
    
    SolrParams params = new ModifiableSolrParams();
    XJoinResults<String> results = factory.getResults(params);
    
    assertEquals(0, IteratorUtils.toArray(results.getJoinIds().iterator()).length);
  }
  
  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testJoinIdSubstitution() throws IOException {
    NamedList args = new NamedList();
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_TYPE, SimpleXJoinResultsFactory.Type.JSON.toString());
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_ROOT_URL, getClass().getResource("results.json").toString());
    
    NamedList globalPaths = new NamedList();
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_GLOBAL_FIELD_PATHS, globalPaths);
    globalPaths.add("total", "$.count");
    
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_JOIN_ID_PATH, "$.hits[*].id");

    args.add(SimpleXJoinResultsFactory.INIT_PARAM_JOIN_ID_TOKEN, "%%");
    
    NamedList resultPaths = new NamedList();
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_RESULT_FIELD_PATHS, resultPaths);
    resultPaths.add("colour", "$.hits[?(@.id == '%%')].colour");
    resultPaths.add("value", "$.meta[?(@.id == '%%')].value");
    
    testResultsFile(args, true, true);
  }
  
  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testNoGlobalPaths() throws IOException {
    NamedList args = new NamedList();
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_TYPE, SimpleXJoinResultsFactory.Type.XML.toString());
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_ROOT_URL, getClass().getResource("results.xml").toString());
    
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_JOIN_ID_PATH, "/results/hits/doc/@id");
    
    NamedList resultPaths = new NamedList();
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_RESULT_FIELD_PATHS, resultPaths);
    resultPaths.add("colour", "/results/hits/doc[@id='JOINID']/colour");
    resultPaths.add("value", "/results/meta/doc[@id='JOINID']/@value");
    
    testResultsFile(args, false, true);    
  }
  
  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testNoResultPaths() throws IOException {
    NamedList args = new NamedList();
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_TYPE, SimpleXJoinResultsFactory.Type.JSON.toString());
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_ROOT_URL, getClass().getResource("results.json").toString());
    
    NamedList globalPaths = new NamedList();
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_GLOBAL_FIELD_PATHS, globalPaths);
    globalPaths.add("total", "$.count");
    
    args.add(SimpleXJoinResultsFactory.INIT_PARAM_JOIN_ID_PATH, "$.hits[*].id");
    
    testResultsFile(args, true, false);
  }
  
}
