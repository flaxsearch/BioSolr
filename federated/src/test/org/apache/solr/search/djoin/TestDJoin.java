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

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.TestHarnessWrapper;
import org.junit.BeforeClass;
import org.junit.Test;

import sun.misc.IOUtils;

/**
 * Four cores, one for each of three 'shards', one for aggregator, which is the core of the extended test case.
 */
public class TestDJoin extends SolrTestCaseJ4 {

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
  
  // re-use same solr home and config string as aggregator; load with documents
  private static void loadShardCore(String coreName, String[][] documents) throws Exception {
    TestHarnessWrapper w = new TestHarnessWrapper(h, coreName);
    for (String[] doc : documents) {
      assertNull(w.validateUpdate(adoc("id", doc[0], "letter", doc[1])));
    }
    assertNull(w.validateUpdate(commit()));
  }
  
  @BeforeClass
  public static void beforeClass() throws Exception {
    try (InputStream in = ClassLoader.getSystemResourceAsStream("djoin/solr/solr-djoin.xml")) {
      byte[] encoded = IOUtils.readFully(in, -1, true);
      createCoreContainer("djoin/solr", new String(encoded, "UTF-8"));
    }
    
    loadShardCore("shard1", DOCUMENTS_1);
    loadShardCore("shard2", DOCUMENTS_2);
    loadShardCore("shard3", DOCUMENTS_3);
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
  
      SolrQueryResponse rsp = new SolrQueryResponse();
      SolrQueryRequest req = new SolrQueryRequestBase(core, params) { };
      try {
        SolrRequestHandler handler = core.getRequestHandler("djoin");
        core.execute(handler, req, rsp);
      } finally {
        req.close();
      }

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
