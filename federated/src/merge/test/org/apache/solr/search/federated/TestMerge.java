package org.apache.solr.search.federated;

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

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.federated.BaseTestCase;
import org.apache.solr.search.federated.MergeException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Four cores, one for each of three 'shards' ("shard1", "shard2", "shard3"), and one for aggregator ("djoin").
 */
public class TestMerge extends BaseTestCase {
  
  private final static String[] FIELDS = new String[] {
    "id", "letter", "single", "text",
    "ignore", "required", "singlevalued",
    "xyzzy", "default", "notanumber"
  };

  private final static String[][] DOCUMENTS_1 = new String[][] {
    { "1", "D", "x", "foo", "_", "_", "@", "plugh", null },
    { "3", "Q", "z", "foo", "_", "_", "@", "plover", null } };

  private final static String[][] DOCUMENTS_2 = new String[][] {
    { "1", "A", null, "foo,bar", "_", "_", "@", null, null },
    { "2", "B", "y", "foo", "_", "_", "@", "fee", null },
    { "3", "C", "z", "foo", "_", "_", "@", null, null } };

  private final static String[][] DOCUMENTS_3 = new String[][] {
    { "1", "E", null, "bar,baz", "_", "_", "@", null, null },
    { "2", "B", "y", "foo", "_", "_", "@", null, null },
    { "3", "C", "z", "foo", "_", "_", "@", null, null } };

  private final static String[][] DOCUMENTS_4 = new String[][] {
    { "1", "E", "w", "foo", null, null, "!", null, "xxx", "NaN" } };

  @BeforeClass
  public static void beforeClass() throws Exception {
    initCores("federated/solr/solr-merge.xml", "federated/solr");
    loadShardCore("shard1", DOCUMENTS_1, FIELDS);
    loadShardCore("shard2", DOCUMENTS_2, FIELDS);
    loadShardCore("shard3", DOCUMENTS_3, FIELDS);
    loadShardCore("shard4", DOCUMENTS_4, FIELDS);
  }
  
  /**
   * Docs from each shard should be 'grouped' by unique id and appear in order by least letter.
   * 
   * Single-valued fields should accept multiple identical values.
   * 
   * Required fields should have a value from a least one shard, but not necessarily all.
   */
  @Test
  public void testMerge() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("sort", "letter asc");
      params.add("fl", "*");

      SolrDocumentList docs = queryDocs(core, "merge", params);
      assertEquals(3, docs.size());
      
      SolrDocument doc0 = docs.get(0);
      assertEquals("1", doc0.getFieldValue("id"));
      assertEquals(new HashSet<String>(Arrays.asList("A", "D", "E")), doc0.getFieldValue("letter"));
      
      SolrDocument doc1 = docs.get(1);
      assertEquals("2", doc1.getFieldValue("id"));
      assertEquals(new HashSet<String>(Arrays.asList("B")), doc1.getFieldValue("letter"));
      
      SolrDocument doc2 = docs.get(2);
      assertEquals("3", doc2.getFieldValue("id"));
      assertEquals(new HashSet<String>(Arrays.asList("C", "Q")), doc2.getFieldValue("letter"));
    }
  }
  
  /**
   * Fields not defined in the aggregator schema (but are present in shards) should not appear.
   */
  @Test
  public void testNotDefined() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("fl", "*");

      SolrDocumentList docs = queryDocs(core, "merge", params);
      assertEquals(3, docs.size());
      
      for (SolrDocument doc : docs) {
        assertNull(doc.getFieldValue("ignore"));
      }
    }
  }
  
  /**
   * Single-valued fields should not accept multiple different values.
   */
  @Test(expected=MergeException.FieldNotMultiValued.class)
  public void testSingleValued() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("shards", "shard1/,shard4/");
      params.add("fl", "single");

      queryThrow(core, "merge", params);
    }
  }
  
  /**
   * Copy-field directives in the aggregator schema should be respected.
   */
  @Test
  public void testCopyFields() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("sort", "letter asc");
      params.add("fl", "*");

      SolrDocumentList docs = queryDocs(core, "merge", params);
      assertEquals(3, docs.size());
      
      SolrDocument doc0 = docs.get(0);
      assertEquals("x", doc0.getFieldValue("copy"));
      assertEquals(new HashSet<String>(Arrays.asList("A", "D", "E", "x")), doc0.getFieldValue("multicopy"));
      
      SolrDocument doc1 = docs.get(1);
      assertEquals("y", doc1.getFieldValue("copy"));
      assertEquals(new HashSet<String>(Arrays.asList("B", "y")), doc1.getFieldValue("multicopy"));
      
      SolrDocument doc2 = docs.get(2);
      assertEquals("z", doc2.getFieldValue("copy"));
      assertEquals(new HashSet<String>(Arrays.asList("C", "Q", "z")), doc2.getFieldValue("multicopy"));
    }   
  }
  
  /**
   * Required fields should expect a value from at least one shard.
   */
  @Test(expected=MergeException.MissingRequiredField.class)
  public void testRequired() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("shards", "shard2/,shard3/");
      params.add("fl", "*");

      queryThrow(core, "merge", params);
    }   
  }
  
  /**
   * All values from shard's multi-valued fields should be present in the merged field.
   */
  @Test
  public void testMultiValued() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("sort", "letter asc");
      params.add("fl", "text");

      SolrDocumentList docs = queryDocs(core, "merge", params);
      assertEquals(3, docs.size());
      
      SolrDocument doc0 = docs.get(0);
      assertEquals(new HashSet<String>(Arrays.asList("foo", "bar", "baz")), doc0.getFieldValue("text"));
    }   
  }
  
  /**
   * Fields should not be returned if they are non-stored, even if asked for.
   */
  @Test
  public void testNonStored() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("sort", "letter asc");
      params.add("fl", "required");

      SolrDocumentList docs = queryDocs(core, "merge", params);
      assertEquals(3, docs.size());
      
      SolrDocument doc0 = docs.get(0);
      assertNull(doc0.getFieldValue("required"));
    }   
  }
  
  /**
   * Fields should be checked for requiredness even if not stored (but only if in field list).
   */
  @Test(expected=MergeException.MissingRequiredField.class)
  public void testNonStoredRequired() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("shards", "shard4/");
      params.add("fl", "required");

      queryThrow(core, "merge", params);
    }   
  }
  
  /**
   * Fields should be checked for single-valuedness even if not stored (but only if in field list).
   */
  @Test(expected=MergeException.FieldNotMultiValued.class)
  public void testNonStoredSingleValued() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("shards", "shard1/,shard2/,shard3/,shard4/");
      params.add("fl", "singlevalued");

      queryThrow(core, "merge", params);
    }   
  }
  
  /**
   * Default values in the schema should be used if no value is returned from shards.
   */
  @Test
  public void testDefaultValue() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("sort", "letter asc");
      params.add("fl", "default");

      SolrDocumentList docs = queryDocs(core, "merge", params);
      assertEquals(3, docs.size());
      
      SolrDocument doc0 = docs.get(0);
      assertEquals("foo", doc0.getFieldValue("default"));
    }   
  }
  
  /**
   * Default values in the schema should not override existing values.
   */
  @Test
  public void testDefaultExists() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("shards", "shard4/");
      params.add("fl", "default");

      SolrDocumentList docs = queryDocs(core, "merge", params);
      assertEquals(1, docs.size());
      
      SolrDocument doc0 = docs.get(0);
      assertEquals("xxx", doc0.getFieldValue("default"));
    }   
  }
  
  /**
   * When [shard] is in the field list, it should be returned (and correct!) (but no score).
   */
  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testWantsShard() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("sort", "letter asc");
      params.add("fl", "*,[shard]");

      SolrDocumentList docs = queryDocs(core, "merge", params);
      assertEquals(3, docs.size());
      
      NamedList[] nls = new NamedList[3];
      for (int i = 0; i < nls.length; ++i) {
        nls[i] = new NamedList();
        nls[i].add("address", "shard" + (i + 1) + "/");
      }

      SolrDocument doc0 = docs.get(0);
      assertEquals(new HashSet<NamedList>(Arrays.asList(nls[0], nls[1], nls[2])), doc0.getFieldValue("[shard]"));
      assertNull(doc0.getFieldValue("score"));
      
      SolrDocument doc1 = docs.get(1);
      assertEquals(new HashSet<NamedList>(Arrays.asList(nls[1], nls[2])), doc1.getFieldValue("[shard]"));
      assertNull(doc1.getFieldValue("score"));
      
      SolrDocument doc2 = docs.get(2);
      assertEquals(new HashSet<NamedList>(Arrays.asList(nls[0], nls[1], nls[2])), doc2.getFieldValue("[shard]"));
      assertNull(doc2.getFieldValue("score"));
    }   
  }
  
  /**
   * When [shard],score is in the field list, both should be returned (and correct!).
   */
  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testWantsShardAndScore() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("sort", "letter asc");
      params.add("fl", "*,[shard],score");

      SolrDocumentList docs = queryDocs(core, "merge", params);
      assertEquals(3, docs.size());
      
      NamedList[] nls = new NamedList[3];
      for (int i = 0; i < nls.length; ++i) {
        nls[i] = new NamedList();
        nls[i].add("address", "shard" + (i + 1) + "/");
        nls[i].add("score", 1.0f);
      }

      SolrDocument doc0 = docs.get(0);
      assertEquals(new HashSet<NamedList>(Arrays.asList(nls[0], nls[1], nls[2])), doc0.getFieldValue("[shard]"));
      assertEquals(1.0f, doc0.getFieldValue("score"));
      
      SolrDocument doc1 = docs.get(1);
      assertEquals(new HashSet<NamedList>(Arrays.asList(nls[1], nls[2])), doc1.getFieldValue("[shard]"));
      assertEquals(1.0f, doc1.getFieldValue("score"));
      
      SolrDocument doc2 = docs.get(2);
      assertEquals(new HashSet<NamedList>(Arrays.asList(nls[0], nls[1], nls[2])), doc2.getFieldValue("[shard]"));
      assertEquals(1.0f, doc2.getFieldValue("score"));
    }   
  }
  
  /**
   * When [shard] is not in the field list, the [shard] field should not be returned.
   */
  @Test
  public void testNotWantsShard() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("sort", "letter asc");
      params.add("fl", "*");

      SolrDocumentList docs = queryDocs(core, "merge", params);
      assertEquals(3, docs.size());
      
      for (SolrDocument doc : docs) {
        assertNull(doc.getFieldValue("[shard]"));
        assertNull(doc.getFieldValue("score"));
      }
    }   
  }
  
  /**
   * When [shard] is not in the field list, the [shard] field should not be returned, even
   * if score is in the field list.
   */
  @Test
  public void testNotWantsShardWithScore() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("sort", "letter asc");
      params.add("fl", "*,score");

      SolrDocumentList docs = queryDocs(core, "merge", params);
      assertEquals(3, docs.size());
      
      for (SolrDocument doc : docs) {
        assertNull(doc.getFieldValue("[shard]"));
        assertEquals(1.0f, doc.getFieldValue("score"));
      }
    }   
  }
  
  
  /**
   * A dynamic field defined in the aggregator schema should accept values.
   */
  @Test
  public void testDynamicField() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("sort", "letter asc");
      params.add("fl", "xyzzy");

      SolrDocumentList docs = queryDocs(core, "merge", params);
      assertEquals(3, docs.size());
      
      SolrDocument doc0 = docs.get(0);
      assertEquals("plugh", doc0.getFieldValue("xyzzy"));
      
      SolrDocument doc1 = docs.get(1);
      assertEquals("fee", doc1.getFieldValue("xyzzy"));
      
      SolrDocument doc2 = docs.get(2);
      assertEquals("plover", doc2.getFieldValue("xyzzy"));
    }   
  }
  
  /**
   * A dynamic copy field should be filled as normal.
   */
  @Test
  public void testDynamicCopyField() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("sort", "letter asc");
      params.add("fl", "copyx");

      SolrDocumentList docs = queryDocs(core, "merge", params);
      assertEquals(3, docs.size());
      
      SolrDocument doc0 = docs.get(0);
      assertEquals("plugh", doc0.getFieldValue("copyx"));
      
      SolrDocument doc1 = docs.get(1);
      assertEquals("fee", doc1.getFieldValue("copyx"));
      
      SolrDocument doc2 = docs.get(2);
      assertEquals("plover", doc2.getFieldValue("copyx"));
    }   
  }
  
  /**
   * The merge should fail if a conversion error occurs.
   */
  @Test(expected=NumberFormatException.class)
  public void testConversionError() throws Exception {
    try (SolrCore core = h.getCoreContainer().getCore("merge")) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.add("q", "*:*");
      params.add("shards", "shard4/");
      params.add("fl", "number");

      queryThrow(core, "merge", params);
    }   
  }
  
}
