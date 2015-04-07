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

public class ChildNodeFacetTreeBuilder implements FacetTreeBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChildNodeFacetTreeBuilder.class);
	
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
		Map<String, FacetEntry> entryMap = 
				entries.stream().collect(Collectors.toMap(FacetEntry::getLabel, Function.identity()));

		// Now collate the nodes into level-based tree(s)
		List<FacetEntry> facetTrees = new ArrayList<>(topUris.size());
		for (String uri : topUris) {
			FacetEntry fe = buildAccumulatedEntryTree(0, annotationMap.get(uri), entryMap, annotationMap);
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
	 * @param entryMap the facet entry map.
	 * @param annotationMap the map of valid annotations (either in the facet map, or parents of
	 * entries in the facet map).
	 * @return an {@link AccumulatedFacetEntry} containing details for the current node and all
	 * sub-nodes down to the lowest leaf which has a facet count.
	 */
	private AccumulatedFacetEntry buildAccumulatedEntryTree(int level, OntologyEntryBean node,
			Map<String, FacetEntry> entryMap, Map<String, OntologyEntryBean> annotationMap) {
		SortedSet<AccumulatedFacetEntry> childHierarchy = new TreeSet<>(Collections.reverseOrder());
		long childTotal = 0;
		if (node.getChildUris() != null) {
			for (String childUri : node.getChildUris()) {
				if (annotationMap.containsKey(childUri)) {
					LOGGER.trace("[{}] Building subAfe for {}", level, childUri);
					AccumulatedFacetEntry subAfe = buildAccumulatedEntryTree(level + 1, annotationMap.get(childUri),
							entryMap, annotationMap);
					childTotal += subAfe.getTotalCount();
					childHierarchy.add(subAfe);
					LOGGER.trace("[{}] subAfe total: {} - child Total {}, child count {}", level, subAfe.getTotalCount(), childTotal, childHierarchy.size());
				}
			}
		}

		long count = 0;
		if (entryMap.containsKey(node.getUri())) {
			count = entryMap.get(node.getUri()).getCount();
		}
		
		String label;
		if (node.getLabel() == null) {
			label = node.getShortForm().get(0);
		} else {
			label = node.getLabel().get(0);
		}
		
		LOGGER.trace("[{}] Building AFE for {}", level, node.getUri());
		return new AccumulatedFacetEntry(node.getUri(), label, count, childTotal, childHierarchy);
	}
	
}
