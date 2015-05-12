package uk.co.flax.biosolr;

import java.io.IOException;
import java.util.List;

import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.FacetComponent;
import org.apache.solr.handler.component.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreeFacetComponent extends FacetComponent {

	public static final String FACET_TREE = FacetParams.FACET + ".tree";
	public static final String FACET_TREE_FIELD = FACET_TREE + ".field";
	public static final String FACET_TREE_CHILD_FIELD = FACET_TREE + ".childfield";

	private static final Logger LOGGER = LoggerFactory.getLogger(TreeFacetComponent.class);
	
	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
		super.prepare(rb);
		
		// Do we need to create a facet tree?
		if (rb.doFacets && rb.req.getParams().getBool(FACET_TREE, false)) {
			LOGGER.debug("{} set to true - adding tree fields to facets", FACET_TREE);
			// Make sure the facet tree field is in the facet field list
			addTreeFieldsToFacets(rb);
		}
	}
	
	private void addTreeFieldsToFacets(ResponseBuilder rb) {
		String[] ftFields = rb.req.getParams().getParams(FACET_TREE_FIELD);
		if (ftFields == null || ftFields.length == 0) {
			LOGGER.warn("No facet tree fields specified - ignoring facet trees");
		} else {
			NamedList<Object> params = rb.req.getParams().toNamedList();
			for (String field : ftFields) {
				params.add(FacetParams.FACET_FIELD, field);
				LOGGER.debug("Adding facet tree field {}", field);
			}

			rb.req.setParams(SolrParams.toSolrParams(params));
		}
	}

	@Override
	public void process(ResponseBuilder rb) throws IOException {
		// Handle the initial facets
		super.process(rb);

		// And do the facet tree, if required
		if (rb.doFacets && rb.req.getParams().getBool(FACET_TREE, false)) {
			FacetTreeProcessor ftp = new FacetTreeProcessor(rb.req, rb.getResults().docSet, rb.req.getParams(), rb);
			SimpleOrderedMap<List<SimpleOrderedMap<Object>>> ftpResponse = ftp.process(rb.req.getParams().getParams(FACET_TREE_FIELD));
			
			@SuppressWarnings("unchecked")
			NamedList<Object> facetCounts = (NamedList<Object>) rb.rsp.getValues().get("facet_counts");
			if (facetCounts != null) {
				facetCounts.add("facet_trees", ftpResponse);
			} else {
				facetCounts = new NamedList<>();
				facetCounts.add("facet_trees", ftpResponse);
				rb.rsp.add("facet_counts", facetCounts);
			}
		}
	}
	
}
