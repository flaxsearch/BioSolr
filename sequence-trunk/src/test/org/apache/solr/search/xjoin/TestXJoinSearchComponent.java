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
import org.apache.solr.response.ResultContext;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.junit.Test;

public class TestXJoinSearchComponent extends AbstractXJoinTestCase {

  static String requestHandler = "standard";

  @Test
  @SuppressWarnings("rawtypes")
  public void testUngrouped() {
    ModifiableSolrParams params = new ModifiableSolrParams();
    NamedList results = test(params, "xjoin");
    testXJoinResults(results, "xjoin");
    ResultContext response = (ResultContext)results.get("response");
    DocList docs = response.getDocList();
    assertEquals(2, docs.size());
    DocIterator it = docs.iterator();
    assertTrue(it.hasNext());
    assertEquals(1, it.nextDoc());
    assertTrue(it.hasNext());
    assertEquals(3, it.nextDoc());
    assertFalse(it.hasNext());
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testGrouped() {
    ModifiableSolrParams params = new ModifiableSolrParams();    
    params.add("group", "true");
    params.add("group.field", "colour");
    NamedList results = test(params, "xjoin");
    testXJoinResults(results, "xjoin");
    NamedList grouped = (NamedList)results.get("grouped");
    NamedList colours = (NamedList)grouped.get("colour");
    assertEquals(2, colours.get("matches"));
    List<NamedList> groups = (List<NamedList>)colours.get("groups");
    assertEquals(2, groups.size());
    
    // check first (green) group
    NamedList green = (NamedList)groups.get(0);
    assertEquals(green.get("groupValue"), "green");
    DocList docs = (DocList)green.get("doclist");
    assertEquals(docs.size(), 1);
    DocIterator it = docs.iterator();
    assertTrue(it.hasNext());
    assertEquals(1, it.nextDoc());
    
    // check second (pink) group
    NamedList pink = (NamedList)groups.get(1);
    assertEquals(pink.get("groupValue"), "pink");
    docs = (DocList)pink.get("doclist");
    assertEquals(docs.size(), 1);
    it = docs.iterator();
    assertTrue(it.hasNext());
    assertEquals(3, it.nextDoc());
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void testGroupedSimple() {
    ModifiableSolrParams params = new ModifiableSolrParams();    
    params.add("group", "true");
    params.add("group.field", "colour");
    params.add("group.format", "simple");
    NamedList results = test(params, "xjoin");
    testXJoinResults(results, "xjoin");
    NamedList grouped = (NamedList)results.get("grouped");
    NamedList colours = (NamedList)grouped.get("colour");
    assertEquals(2, colours.get("matches"));
    DocList docs = (DocList)colours.get("doclist");
    assertEquals(docs.size(), 2);
    DocIterator it = docs.iterator();
    assertTrue(it.hasNext());
    assertEquals(1, it.nextDoc());
    assertTrue(it.hasNext());
    assertEquals(3, it.nextDoc());
    assertFalse(it.hasNext());
  }
  
  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testMultiValued() {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.add("xjoin4", "true");
    NamedList results = test(params, "xjoin4");
    NamedList xjoin = (NamedList)results.get("xjoin4");
    List<NamedList> list = (List)xjoin.get("external");
    assertEquals(5, list.size());
    assertEquals(list.get(0).get("joinId"), "alpha");
    assertEquals(list.get(1).get("joinId"), "beta");
    assertEquals(list.get(2).get("joinId"), "gamma");
    assertEquals(list.get(3).get("joinId"), "delta");
    assertEquals(list.get(4).get("joinId"), "theta");
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void testXJoinResults(NamedList results, String componentName) {
    NamedList xjoin = (NamedList)results.get(componentName);
    assertTrue(componentName + " should not be null", xjoin != null);
    assertEquals("a test string", xjoin.get("string"));
    
    String[] values = { "1", "3", "8" };
    Set<String> expected = new HashSet<>(Arrays.asList(values));
    Set<String> actual = new HashSet<String>((List<String>)xjoin.get("join_ids"));
    assertEquals(expected, actual);
  }
  
}
