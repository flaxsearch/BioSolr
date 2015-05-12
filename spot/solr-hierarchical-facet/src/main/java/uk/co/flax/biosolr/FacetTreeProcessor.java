package uk.co.flax.biosolr;

import java.io.IOException;
import java.util.List;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.request.SimpleFacets;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SyntaxError;

public class FacetTreeProcessor extends SimpleFacets {

	public static final String LOCAL_PARAM_TYPE = "ftree";
	public static final String CHILD_FIELD_PARAM = "childField";
	public static final String COLLECTION_PARAM = "collection";
	public static final String NODE_FIELD_PARAM = "nodeField";
	public static final String LABEL_FIELD_PARAM = "labelField";

	public FacetTreeProcessor(SolrQueryRequest req, DocSet docs, SolrParams params, ResponseBuilder rb) {
		super(req, docs, params, rb);
	}

	public SimpleOrderedMap<List<SimpleOrderedMap<Object>>> process(String[] facetTrees) throws IOException {
		if (!rb.doFacets || facetTrees == null || facetTrees.length == 0) {
			return null;
		}

		SimpleOrderedMap<List<SimpleOrderedMap<Object>>> treeResponse = new SimpleOrderedMap<>();
		for (String fTree : facetTrees) {
			String nodeField;
			try {
				// NOTE: this sets localParams (SimpleFacets is stateful)
				this.parseParams(LOCAL_PARAM_TYPE, fTree);
				if (localParams == null) {
					throw new SyntaxError("Missing facet tree parameters");
				} else if (localParams.get(CHILD_FIELD_PARAM) == null) {
					throw new SyntaxError("Missing child field definition in " + fTree);
				} else if (localParams.get(NODE_FIELD_PARAM) == null) {
					nodeField = key;
				} else {
					nodeField = localParams.get(NODE_FIELD_PARAM);
				}
			} catch (SyntaxError e) {
				throw new SolrException(ErrorCode.BAD_REQUEST, e);
			}
			
			// Construct a generator for the fields we want
			FacetTreeGenerator generator = new FacetTreeGenerator(localParams.get(COLLECTION_PARAM), nodeField,
					localParams.get(CHILD_FIELD_PARAM), localParams.get(LABEL_FIELD_PARAM));
			// Generate the tree
			List<SimpleOrderedMap<Object>> trees = generator.generateTree(rb, getTermCounts(key));

			// Add the generated tree to the response
			treeResponse.add(key, trees);
		}

		return treeResponse;
	}

}
