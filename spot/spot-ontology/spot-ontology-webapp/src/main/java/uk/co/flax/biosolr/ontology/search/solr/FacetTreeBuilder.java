package uk.co.flax.biosolr.ontology.search.solr;

import java.util.List;

import uk.co.flax.biosolr.ontology.api.AccumulatedFacetEntry;
import uk.co.flax.biosolr.ontology.api.FacetEntry;

/**
 * A facet tree is a hierarchical structure containing all of the
 * facets in a result set, arranged into a hierarchical order up to the
 * highest common parent node in the set. 
 */
public interface FacetTreeBuilder {

	/**
	 * Convert the incoming facet list into a list comprising one or more
	 * {@link AccumulatedFacetEntry} objects.
	 * @param entries the initial facets.
	 * @return the accumulated, hierarchical facet entries.
	 */
	public List<FacetEntry> buildFacetTree(List<FacetEntry> entries);

}
