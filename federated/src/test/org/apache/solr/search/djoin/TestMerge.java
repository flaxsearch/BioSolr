package org.apache.solr.search.djoin;

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
import org.apache.solr.search.djoin.DuplicateDocumentList;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMerge  extends SolrTestCaseJ4 {

  private final static String[][] DOCUMENTS = new String[][] {
    { "shard/1/1", "D" }, // shard/[shard n]/[doc id]
    { "shard/1/3", "Q" },
    { "shard/2/1", "A" },
    { "shard/2/2", "B" },
    { "shard/2/3", "C" },
    { "shard/3/1", "E" },
    { "shard/3/2", "B" },
    { "shard/3/3", "C" } };
  
  @BeforeClass
  public static void beforeClass() throws Exception {
    initCore("solrconfig.xml", "schema.xml", "djoin/solr");
   
    for (String[] doc : DOCUMENTS) {
      assertNull(h.validateUpdate(adoc("id", doc[0], "letter", doc[1])));
    }
    assertNull(h.validateUpdate(commit()));
  }
  
  /**
   * Test that (a) sorting works correctly, so that we get docs 1 and 2 returned (sort values A and B)
   *       and (b) we still get all parts of doc 1 returned even though it ranks third on shard/3
   */
  @Test
  public void test() throws Exception {
    SolrCore core = h.getCore();
    
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.add("q", "*:*");
    params.add("rows", "2");
    params.add("sort", "letter asc");
    
    SolrQueryResponse rsp = new SolrQueryResponse();
    SolrQueryRequest req = new SolrQueryRequestBase(core, params) {};
    SolrRequestHandler handler = core.getRequestHandler("djoin");
    core.execute(handler, req, rsp);
    req.close();

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
