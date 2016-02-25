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

import java.util.List;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.response.ResultContext;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.xjoin.AbstractXJoinTestCase;
import org.junit.Test;

public class TestSimple extends AbstractXJoinTestCase {
  
  @Test
  @SuppressWarnings("rawtypes")
  public void testSimpleXJoinResultsFactory() {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.add("fl", "*, score");
    params.add("xjoin", "false");
    params.add("xjoin5", "true");
    params.add("xjoin5.fl", "*");
    
    // score boosts using value source parser
    params.add("defType", "edismax");
    params.add("bf", "xjoin5(value)");
    
    NamedList results = test(params, "xjoin5");
    ResultContext response = (ResultContext)results.get("response");
    DocList docs = response.getDocList();
    assertEquals(2, docs.size());
    DocIterator it = docs.iterator();
    assertTrue(it.hasNext());
    assertEquals(0, it.nextDoc());
    double score0 = it.score();
    assertTrue(it.hasNext());
    assertEquals(2, it.nextDoc());
    double score2 = it.score();
    assertFalse(it.hasNext());
 
    // bf score boost for testid=0 only
    assertTrue(score0 > score2);
    
    List externalList = (List)((NamedList)results.get("xjoin5")).get("external");
    NamedList hit0 = (NamedList)externalList.get(0);
    assertEquals("0", hit0.get("joinId"));
    NamedList doc0 = (NamedList)hit0.get("doc");
    assertEquals("red", doc0.get("colour"));
    assertEquals(10.5, doc0.get("value"));
  }
  
}
