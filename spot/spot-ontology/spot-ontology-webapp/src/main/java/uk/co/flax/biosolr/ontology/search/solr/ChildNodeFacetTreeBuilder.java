package uk.co.flax.biosolr.ontology.search.solr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.api.AccumulatedFacetEntry;
import uk.co.flax.biosolr.ontology.api.FacetEntry;
import uk.co.flax.biosolr.ontology.api.OntologyEntryBean;
import uk.co.flax.biosolr.ontology.search.OntologySearch;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

/**
 * <p>
 * Implementation of the {@link FacetTreeBuilder} that only relies on the
 * node ID and child node ID fields to build the hierarchical facet tree.
 * </p>
 * <p>
 * This works from the bottom-level up, searching to find records whose
 * child node list contains the current level's node IDs. The search is
 * repeated until there are no further child nodes to filter on. Once
 * a complete list of records is available, the top-level node(s) are found,
 * and then used as a base to recurse through the remainder of the records,
 * working out where they fall in the hierarchy.
 * </p>
 */
public class ChildNodeFacetTreeBuilder implements FacetTreeBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChildNodeFacetTreeBuilder.class);
	
	/** The fields required when searching for the nodes to use in the tree. */
	private static final List<String> FIELD_LIST = Arrays.asList(
			SolrOntologySearch.URI_FIELD,
			SolrOntologySearch.CHILD_URI_FIELD,
			SolrOntologySearch.LABEL_FIELD,
			SolrOntologySearch.SHORT_FORM_FIELD);
	
	private final OntologySearch ontologySearch;

	public ChildNodeFacetTreeBuilder(OntologySearch ontologySearch) {
		this.ontologySearch = ontologySearch;
	}
	
	@Override
	public List<FacetEntry> buildFacetTree(List<FacetEntry> entries) {
		// Extract the URIs from the facet entries
		Set<String> uriSet = extractIdsFromFacets(entries);
		
		// Find all parent nodes for the incoming URIs
		Map<String, OntologyEntryBean> annotationMap = findParentNodes(uriSet);
		
		// Find the bottom-level nodes, if there are any which haven't been looked up
		uriSet.removeAll(annotationMap.keySet());
		annotationMap.putAll(filterEntriesByField(uriSet, SolrOntologySearch.URI_FIELD));
		
		// Find the top node(s)
		Set<String> topUris = findTopLevelNodes(annotationMap);
		LOGGER.debug("Found {} top level nodes", topUris.size());
		
		// Convert the original facets to a map, keyed by URI
		Map<String, Long> facetCounts = 
				entries.stream().collect(Collectors.toMap(FacetEntry::getLabel, FacetEntry::getCount));

		// Now collate the nodes into level-based tree(s)
		List<FacetEntry> facetTrees = new ArrayList<>(topUris.size());
		for (String uri : topUris) {
			FacetEntry fe = buildAccumulatedEntryTree(0, annotationMap.get(uri), facetCounts, annotationMap);
			facetTrees.add(fe);
		}
		
		return facetTrees;
	}
	
	/**
	 * Find all parent nodes for the given set of URIs.
	 * @param uris the starting set of URIs.
	 * @return a map of nodes, keyed by their URIs.
	 */
	private Map<String, OntologyEntryBean> findParentNodes(Collection<String> uris) {
		Map<String, OntologyEntryBean> parentNodes = new HashMap<>();
		
		Set<String> childrenFound = new HashSet<>();
		Set<String> childUris = new HashSet<>(uris);
		
		while (childUris.size() > 0) {
			// Find the direct parents for the current child URIs
			Map<String, OntologyEntryBean> parents = filterEntriesByField(childUris, SolrOntologySearch.CHILD_URI_FIELD);
			parentNodes.putAll(parents);
			childrenFound.addAll(childUris);
			
			// Get the IDs for all the retrieved nodes - these are the next set of
			// nodes whose parents should be found.
			childUris = parents.keySet();
			// Strip out any nodes we've already looked up
			childUris.removeAll(childrenFound);
		};
		
		return parentNodes;
	}
	
	/**
	 * Extract the label values from the facets and return them as a set.
	 * (Labels are expected to hold the facet IDs.)
	 * @param entries the facets whose labels are required.
	 * @return a set containing all of the labels.
	 */
	private Set<String> extractIdsFromFacets(List<FacetEntry> entries) {
		return entries.stream().map(FacetEntry::getLabel).collect(Collectors.toSet());
	}
	
	/**
	 * Fetch the EFO annotations containing one or more URIs in a particular field.
	 * @param uris the URIs to check for.
	 * @param uriField the field to filter against.
	 * @return a map of URI to ontology entry for the incoming URIs.
	 */
	private Map<String, OntologyEntryBean> filterEntriesByField(Collection<String> uris, String uriField) {
		Map<String, OntologyEntryBean> annotationMap = new HashMap<>();
		if (uris.size() > 0) {
			LOGGER.debug("Looking up {} ontology entries in field {}", uris.size(), uriField);
			String query = "*:*";
			String filters = buildFilterString(uriField, uris);

			try {
				ResultsList<OntologyEntryBean> results = ontologySearch.searchOntology(
						query, Arrays.asList(filters), 0, uris.size(), FIELD_LIST);
				annotationMap = results.getResults().stream().collect(Collectors.toMap(OntologyEntryBean::getUri, Function.identity()));
			} catch (SearchEngineException e) {
				LOGGER.error("Problem getting ontology entries for filter {}: {}", filters, e.getMessage());
			}
		}
		
		return annotationMap;
	}
	
	/**
	 * Build a filter string for a set of URIs.
	 * @param uris
	 * @return a filter string.
	 */
	private String buildFilterString(String field, Collection<String> uris) {
		StringBuilder sb = new StringBuilder(field).append(":(");
		
		int idx = 0;
		for (String uri : uris) {
			if (idx > 0) {
				sb.append(" OR ");
			}
			
			sb.append("\"").append(uri).append("\"");
			idx ++;
		}
		sb.append(")");
		
		return sb.toString();
	}
	
	/**
	 * Find all of the top-level records in a set of annotations. This is done by ooping
	 * through the annotations and finding any other annotations in the set which
	 * contain the current annotation in their child list.
	 * @param annotations a map of annotation ID to annotation entries to check over.
	 * @return a set containing the identifiers for all of the top-level
	 * annotations found.
	 */
	private Set<String> findTopLevelNodes(Map<String, OntologyEntryBean> annotations) {
		Set<String> topLevel = new HashSet<>();

		for (String uri : annotations.keySet()) {
			boolean found = false;
			
			// Check each annotation in the set to see if this
			// URI is in their child list
			for (OntologyEntryBean anno : annotations.values()) {
				if (anno.getChildUris() != null && anno.getChildUris().contains(uri)) {
					// URI is in the child list - not top-level
					found = true;
					break;
				}
			}
			
			if (!found) {
				// URI Is not in any child lists - must be top-level
				topLevel.add(uri);
			}
		}
		
		return topLevel;
	}
	
	/**
	 * Recursively build an accumulated facet entry tree.
	 * @param level current level in the tree (used for debugging/logging).
	 * @param node the current node.
	 * @param facetCounts the facet counts, keyed by node ID.
	 * @param annotationMap the map of annotations (either in the original facet set,
	 * or parent entries of those entries).
	 * @return an {@link AccumulatedFacetEntry} containing details for the current node and all
	 * sub-nodes down to the lowest leaf which has a facet count.
	 */
	private AccumulatedFacetEntry buildAccumulatedEntryTree(int level, OntologyEntryBean node,
			Map<String, Long> facetCounts, Map<String, OntologyEntryBean> annotationMap) {
		// Build the child hierarchy for this entry
		SortedSet<AccumulatedFacetEntry> childHierarchy = new TreeSet<>(Collections.reverseOrder());
		long childTotal = 0;
		if (node.getChildUris() != null) {
			// Loop through all the direct child URIs, looking for those which are in the annotation map
			for (String childUri : node.getChildUris()) {
				if (annotationMap.containsKey(childUri)) {
					// Found a child of this node - recurse to build its facet tree
					LOGGER.trace("[{}] Building subAfe for {}", level, childUri);
					AccumulatedFacetEntry subAfe = buildAccumulatedEntryTree(level + 1, annotationMap.get(childUri),
							facetCounts, annotationMap);
					// childTotal is the total facet hits below the current level
					childTotal += subAfe.getTotalCount();
					childHierarchy.add(subAfe);
					LOGGER.trace("[{}] subAfe total: {} - child Total {}, child count {}", level, subAfe.getTotalCount(), childTotal, childHierarchy.size());
				}
			}
		}

		// Get the count and label for this entry
		long count = getFacetCount(node.getUri(), facetCounts);
		String label = getLabelForNode(node);
		
		// Build the accumulated facet entry
		LOGGER.trace("[{}] Building AFE for {}", level, node.getUri());
		return new AccumulatedFacetEntry(node.getUri(), label, count, childTotal, childHierarchy);
	}
	
	/**
	 * Get the count for the facet with the given key.
	 * @param key the key to look up.
	 * @param facetCounts the map of facet counts.
	 * @return the count, or <code>0</code> if the key does not exist in the map.
	 */
	private long getFacetCount(String key, Map<String, Long> facetCounts) {
		long ret = 0;
		
		if (facetCounts.containsKey(key)) {
			ret = facetCounts.get(key);
		}
		
		return ret;
	}
	
	/**
	 * Get the label for a node.
	 * @param node the node whose label is required.
	 * @return the label - either the first value in the label list, or the
	 * first value in the shortForm list, or the URI if neither of those is
	 * available.
	 */
	private String getLabelForNode(OntologyEntryBean node) {
		String label = node.getUri();
		
		if (node.getLabel() != null && !node.getLabel().isEmpty()) {
			label = node.getLabel().get(0);
		} else if (node.getShortForm() != null && !node.getShortForm().isEmpty()) {
			label = node.getShortForm().get(0);
		}
		
		return label;
	}
	
}
