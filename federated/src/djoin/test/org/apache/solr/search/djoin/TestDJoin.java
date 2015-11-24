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
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Four cores, one for each of three 'shards' ("shard1", "shard2", "shard3"), and one for aggregator ("djoin").
 */
public class TestDJoin extends BaseTestCase {

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
    initCores("djoin/solr/solr-djoin.xml", "djoin/solr");
    loadShardCore("shard1", DOCUMENTS_1, "id", "letter");
    loadShardCore("shard2", DOCUMENTS_2, "id", "letter");
    loadShardCore("shard3", DOCUMENTS_3, "id", "letter");
  }
  
  /**
   * Test that (a) sorting works correctly, so that we get docs 1 and 2 returned (sort values A and B)
   *       and (b) we still get all parts of doc 1 returned even though it ranks third on shard/3
   */
  @Test
  public void testJoin() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("djoin")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("rows", "2");
      params.add("sort", "letter asc");
      params.add("fl", "*,[shard]");

      SolrQueryResponse rsp = query(core, "djoin", params);
      assertNull(rsp.getException());
      SolrDocumentList docs = (SolrDocumentList)rsp.getValues().get("response");
      assertEquals(2, docs.size());
      
      assertEquals(true, docs.get(0).get(DuplicateDocumentList.MERGE_PARENT_FIELD));
      assertEquals(3, docs.get(0).getChildDocumentCount());
      assertEquals("1", docs.get(0).getChildDocuments().get(0).get("id"));
      assertEquals("1", docs.get(0).getChildDocuments().get(1).get("id"));
      assertEquals("1", docs.get(0).getChildDocuments().get(2).get("id"));
      Set<String> letters = new HashSet<>();
      for (SolrDocument doc : docs.get(0).getChildDocuments()) {
        letters.add((String)doc.get("letter"));
      }
      assertEquals(new HashSet<String>(Arrays.asList("A", "D", "E")), letters);
      
      assertEquals(true, docs.get(1).get(DuplicateDocumentList.MERGE_PARENT_FIELD));
      assertEquals(2, docs.get(1).getChildDocumentCount());
      assertEquals("2", docs.get(1).getChildDocuments().get(0).get("id"));
      assertEquals("B", docs.get(1).getChildDocuments().get(0).get("letter"));
      assertEquals("2", docs.get(1).getChildDocuments().get(1).get("id"));
      assertEquals("B", docs.get(1).getChildDocuments().get(1).get("letter"));
    }
  }

}
