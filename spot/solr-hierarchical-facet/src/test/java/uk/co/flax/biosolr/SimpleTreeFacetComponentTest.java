package uk.co.flax.biosolr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.BeforeClass;
import org.junit.Test;

public class SimpleTreeFacetComponentTest extends SolrTestCaseJ4 {
	
	static String requestHandler = "facetTree";
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		// Initialise a single Solr core
		initCore("solrconfig.xml", "schema.xml", "facetTree/solr", "hierarchy");
		
		// Add some records
		assertNull(h.validateUpdate(adoc("id", "0", "node_id", "A", "child_ids", "AA", "child_ids", "AB", "child_ids", "AC", "name", "nodeA", "label", "nodeA")));
		assertNull(h.validateUpdate(adoc("id", "1", "node_id", "AA", "child_ids", "AAA", "child_ids", "AAB", "name", "nodeAA", "label", "nodeAA")));
		assertNull(h.validateUpdate(adoc("id", "2", "node_id", "AAA", "name", "nodeAAA", "label", "nodeAAA")));
		assertNull(h.validateUpdate(adoc("id", "3", "node_id", "AAB", "name", "nodeAAB", "label", "nodeAAB")));
		assertNull(h.validateUpdate(adoc("id", "4", "node_id", "AB", "name", "nodeAB", "label", "nodeAB")));
		assertNull(h.validateUpdate(adoc("id", "5", "node_id", "AC", "name", "nodeAC", "label", "nodeAC")));
		assertNull(h.validateUpdate(commit()));
	}
	
	@SuppressWarnings("rawtypes")
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

	@SuppressWarnings("rawtypes")
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

}
