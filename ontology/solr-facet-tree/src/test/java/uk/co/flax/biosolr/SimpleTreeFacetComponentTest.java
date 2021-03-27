package uk.co.flax.biosolr;

import java.net.URL;
import java.util.List;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for the SimpleTreeFacetComponent, using a basic
 * Solr instance for integration testing.
 *
 * @author mlp
 */
public class SimpleTreeFacetComponentTest extends SolrTestCaseJ4 {
	
	static String requestHandler = "facetTree";
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		// Initialise a single Solr core
		initCore("solrconfig.xml", "schema.xml", "src/test/resources/facetTree/solr", "hierarchy");
		
		// Add some records
		assertNull(h.validateUpdate(adoc("id", "0", "node_id", "A", "child_ids", "AA", "child_ids", "AB", "child_ids", "AC", "name", "nodeA", "label", "nodeA")));
		assertNull(h.validateUpdate(adoc("id", "1", "node_id", "AA", "child_ids", "AAA", "child_ids", "AAB", "name", "nodeAA", "label", "nodeAA")));
		assertNull(h.validateUpdate(adoc("id", "2", "node_id", "AAA", "name", "nodeAAA", "label", "nodeAAA")));
		assertNull(h.validateUpdate(adoc("id", "3", "node_id", "AAB", "name", "nodeAAB", "label", "nodeAAB")));
		assertNull(h.validateUpdate(adoc("id", "4", "node_id", "AB", "name", "nodeAB", "label", "nodeAB")));
		assertNull(h.validateUpdate(adoc("id", "5", "node_id", "AC", "name", "nodeAC", "label", "nodeAC")));
		assertNull(h.validateUpdate(commit()));
	}
	
	@Test
	public void testBadRequest_missingLocalParams() {
		SolrCore core = h.getCore();
		
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.add("q", "*:*");
		params.add("facet", "true");
		params.add("facet.tree", "true");
		params.add("facet.tree.field", "node_id");
		
	    SolrQueryResponse rsp = new SolrQueryResponse();
	    rsp.add("responseHeader", new SimpleOrderedMap<>());
	    SolrQueryRequest req = new LocalSolrQueryRequest(core, params);

	    SolrRequestHandler handler = core.getRequestHandler(requestHandler);
	    handler.handleRequest(req, rsp);
	    req.close();
	      
	    assertNotNull(rsp.getException());
	}

	@Test
	public void testBadRequest_missingChildField() {
		SolrCore core = h.getCore();
		
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.add("q", "*:*");
		params.add("facet", "true");
		params.add("facet.tree", "true");
		params.add("facet.tree.field", "{!ftree childField=blah}node_id");
		
	    SolrQueryResponse rsp = new SolrQueryResponse();
	    rsp.add("responseHeader", new SimpleOrderedMap<>());
	    SolrQueryRequest req = new LocalSolrQueryRequest(core, params);

	    SolrRequestHandler handler = core.getRequestHandler(requestHandler);
	    handler.handleRequest(req, rsp);
	    req.close();
	      
	    assertNotNull(rsp.getException());
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testSingleResult() {
		SolrCore core = h.getCore();
		
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.add("q", "name:nodeAAA");
		params.add("facet", "true");
		params.add("facet.tree", "true");
		params.add("facet.tree.field", "{!ftree childField=child_ids}node_id");
		
	    SolrQueryResponse rsp = new SolrQueryResponse();
	    rsp.add("responseHeader", new SimpleOrderedMap<>());
	    SolrQueryRequest req = new LocalSolrQueryRequest(core, params);

	    SolrRequestHandler handler = core.getRequestHandler(requestHandler);
	    handler.handleRequest(req, rsp);
	    req.close();
	      
	    assertNull(rsp.getException());
	    
	    NamedList results = rsp.getValues();
	    NamedList facetTree = (NamedList) ((NamedList)(results.get("facet_counts"))).get("facet_trees");
	    assertNotNull(facetTree);
	    
	    // Check the generated hierarchy - should be three levels deep
	    List<Object> nodes = (List) facetTree.get("node_id");
	    assertEquals(1, nodes.size());
	    NamedList level1 = (NamedList) nodes.get(0);
	    assertEquals("A", level1.get("value"));
	    assertEquals(0L, level1.get("count"));
	    assertEquals(1L, level1.get("total"));
	    List level2Nodes = (List)level1.get("hierarchy");
	    NamedList level2 = (NamedList)level2Nodes.get(0);
	    assertEquals("AA", level2.get("value"));
	    assertEquals(0L, level2.get("count"));
	    assertEquals(1L, level2.get("total"));
	    List level3Nodes = (List)level2.get("hierarchy");
	    NamedList level3 = (NamedList)level3Nodes.get(0);
	    assertEquals("AAA", level3.get("value"));
	    assertEquals(1L, level3.get("count"));
	    assertEquals(1L, level3.get("total"));
	    assertNull(level3.get("hierarchy"));
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testMultipleResults() {
		SolrCore core = h.getCore();
		
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.add("q", "name:nodeA*");
		params.add("facet", "true");
		params.add("facet.tree", "true");
		params.add("facet.tree.field", "{!ftree childField=child_ids}node_id");
		
	    SolrQueryResponse rsp = new SolrQueryResponse();
	    rsp.add("responseHeader", new SimpleOrderedMap<>());
	    SolrQueryRequest req = new LocalSolrQueryRequest(core, params);

	    SolrRequestHandler handler = core.getRequestHandler(requestHandler);
	    handler.handleRequest(req, rsp);
	    req.close();
	      
	    assertNull(rsp.getException());
	    
	    NamedList results = rsp.getValues();
	    NamedList facetTree = (NamedList) ((NamedList)(results.get("facet_counts"))).get("facet_trees");
	    assertNotNull(facetTree);
	    
	    // Check the generated hierarchy - should be three levels deep
	    List<Object> nodes = (List) facetTree.get("node_id");
	    assertEquals(1, nodes.size());
	    NamedList level1 = (NamedList) nodes.get(0);
	    assertEquals("A", level1.get("value"));
	    assertEquals(1L, level1.get("count"));
	    assertEquals(6L, level1.get("total"));
	    List level2Nodes = (List)level1.get("hierarchy");
	    assertEquals(3, level2Nodes.size());
	    NamedList level2 = (NamedList)level2Nodes.get(0);
	    assertEquals(1L, level2.get("count"));
	    assertEquals(3L, level2.get("total"));
	    List level3Nodes = (List)level2.get("hierarchy");
	    assertEquals(2, level3Nodes.size());
	    NamedList level3 = (NamedList)level3Nodes.get(0);
	    assertEquals(1L, level3.get("count"));
	    assertEquals(1L, level3.get("total"));
	    assertNull(level3.get("hierarchy"));
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testResultsWithOtherFacets() {
		SolrCore core = h.getCore();
		
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.add("q", "name:nodeA*");
		params.add("facet", "true");
		params.add("facet.tree", "true");
		params.add("facet.tree.field", "{!ftree childField=child_ids}node_id");
		params.add("facet.field", "label");
		params.add("facet.field", "node_id");
		
	    SolrQueryResponse rsp = new SolrQueryResponse();
	    rsp.add("responseHeader", new SimpleOrderedMap<>());
	    SolrQueryRequest req = new LocalSolrQueryRequest(core, params);

	    SolrRequestHandler handler = core.getRequestHandler(requestHandler);
	    handler.handleRequest(req, rsp);
	    req.close();
	      
	    assertNull(rsp.getException());
	    
	    NamedList results = rsp.getValues();
	    
	    NamedList facetFields = (NamedList)((NamedList)(results.get("facet_counts"))).get("facet_fields");
	    assertNotNull(facetFields);
	    assertNotNull(facetFields.get("label"));
	    assertNotNull(facetFields.get("node_id"));
	    
	    NamedList facetTree = (NamedList) ((NamedList)(results.get("facet_counts"))).get("facet_trees");
	    assertNotNull(facetTree);
	    
	    // Check the generated hierarchy - should be three levels deep
	    List<Object> nodes = (List) facetTree.get("node_id");
	    assertEquals(1, nodes.size());
	    NamedList level1 = (NamedList) nodes.get(0);
	    assertEquals("A", level1.get("value"));
	    assertEquals(1L, level1.get("count"));
	    assertEquals(6L, level1.get("total"));
	    List level2Nodes = (List)level1.get("hierarchy");
	    assertEquals(3, level2Nodes.size());
	    NamedList level2 = (NamedList)level2Nodes.get(0);
	    assertEquals(1L, level2.get("count"));
	    assertEquals(3L, level2.get("total"));
	    List level3Nodes = (List)level2.get("hierarchy");
	    assertEquals(2, level3Nodes.size());
	    NamedList level3 = (NamedList)level3Nodes.get(0);
	    assertEquals(1L, level3.get("count"));
	    assertEquals(1L, level3.get("total"));
	    assertNull(level3.get("hierarchy"));
	}
	
}
