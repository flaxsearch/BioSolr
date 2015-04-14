package uk.co.flax.biosolr;

import java.io.IOException;

import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.FacetComponent;
import org.apache.solr.handler.component.ResponseBuilder;

public class TreeFacetComponent extends FacetComponent {

	public static final String FACET_TREE = FacetParams.FACET + ".tree";
	public static final String FACET_TREE_FIELD = FACET_TREE + ".field";
	public static final String FACET_TREE_CHILD_FIELD = FACET_TREE + ".childfield";

	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
		super.prepare(rb);
	}

	@Override
	public void process(ResponseBuilder rb) throws IOException {
		// Handle the initial facets
		super.process(rb);

		// And do the facet tree, if required
		if (rb.doFacets && rb.req.getParams().getParams(FACET_TREE) != null) {
			FacetTreeProcessor ftp = new FacetTreeProcessor(rb.req, rb.getResults().docSet, rb.req.getParams(), rb);
			NamedList<Object> ftpResponse = ftp.process(rb.req.getParams().getParams(FACET_TREE));
			
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
