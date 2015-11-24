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

import java.util.Arrays;
import java.util.HashSet;

import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Four cores, one for each of three 'shards' ("shard1", "shard2", "shard3"), and one for aggregator ("djoin").
 */
public class TestMerge extends BaseTestCase {

  private final static String[][] DOCUMENTS_1 = new String[][] {
    { "1", "D" },
    { "3", "Q" } };

  private final static String[][] DOCUMENTS_2 = new String[][] {
    { "1", "A" },
    { "2", "B" },
    { "3", "C" } };

  private final static String[][] DOCUMENTS_3 = new String[][] {
    { "1", "E" },
    { "2", "B" },
    { "3", "C" } };

  @BeforeClass
  public static void beforeClass() throws Exception {
    initCores("djoin/solr/solr-merge.xml", "djoin/solr");
    loadShardCore("shard1", DOCUMENTS_1, "id", "letter");
    loadShardCore("shard2", DOCUMENTS_2, "id", "letter");
    loadShardCore("shard3", DOCUMENTS_3, "id", "letter");
  }
  
  @Test
  public void testMerge() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("rows", "2");
      params.add("sort", "letter asc");
      params.add("fl", "*,[shard]");

      SolrQueryResponse rsp = query(core, "merge", params);
      assertNull(rsp.getException());
      SolrDocumentList docs = (SolrDocumentList)rsp.getValues().get("response");
      assertEquals(2, docs.size());
      
      assertEquals(0, docs.get(0).getChildDocumentCount());
      assertEquals("1", docs.get(0).get("id"));
      assertEquals(new HashSet<String>(Arrays.asList("A", "D", "E")), docs.get(0).get("letter"));
      
      assertEquals(0, docs.get(1).getChildDocumentCount());
      assertEquals("2", docs.get(1).get("id"));
      assertEquals(new HashSet<String>(Arrays.asList("B")), docs.get(1).get("letter"));
    }
  }

}
