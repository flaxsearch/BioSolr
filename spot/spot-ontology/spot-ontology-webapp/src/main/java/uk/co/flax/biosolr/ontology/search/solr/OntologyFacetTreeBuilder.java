/**
 * Copyright (c) 2015 Lemur Consulting Ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.flax.biosolr.ontology.search.solr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.api.AccumulatedFacetEntry;
import uk.co.flax.biosolr.ontology.api.FacetEntry;
import uk.co.flax.biosolr.ontology.api.OntologyEntryBean;
import uk.co.flax.biosolr.ontology.search.OntologySearch;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

/**
 * Utility class to convert a list of Document EFO annotation facets into a
 * hierarchical tree.
 * 
 * <p>This uses the ontology index to build a full tree of nodes which have
 * descendants contained in the facet list. Note that there are no uniqueness checks
 * done on the leaf nodes - if a node has multiple parents, it may appear more than
 * once, and affect the accumulated total counts accordingly.</p>
 * 
 * @author Matt Pearce
 */
public class OntologyFacetTreeBuilder implements FacetTreeBuilder {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OntologyFacetTreeBuilder.class);
	
	private final OntologySearch ontologySearch;

	public OntologyFacetTreeBuilder(OntologySearch ontologySearch) {
		this.ontologySearch = ontologySearch;
	}
	
	/**
	 * Convert the incoming facet entries into a hierarchical facet tree, starting
	 * from the highest parent node common to all of the facets, and terminating at
	 * the lowest leaf level for each facet.
	 * @param entries the annotation facets, expected to be a list of Ontology URIs.
	 * @return a list containing one or more {@link AccumulatedFacetEntry} items representing
	 * the full tree.
	 */
	@Override
	public List<FacetEntry> buildFacetTree(List<FacetEntry> entries) {
		// Look up ontology entries for every facet in the entry set.
		Map<String, OntologyEntryBean> annotationMap = lookupOntologyEntriesByFacetLabel(entries);
		Map<String, FacetEntry> entryMap = convertEntriesToMap(entries);
		
		// Look up ontology entries for all parent nodes common to the incoming entries
		Set<String> parentUris = extractParentUris(annotationMap.values());
		// Remove URIs for which we already have EFO annotations
		parentUris.removeAll(annotationMap.keySet());
		// Add the parent ontology entries to the facet ontology entries
		annotationMap.putAll(lookupOntologyEntriesByUri(parentUris));
		
		// Build a map of annotations by level in the tree, so we can start at the highest level
		Map<Integer, List<OntologyEntryBean>> levelMap = collateAnnotationsByLevel(annotationMap);
		
		SortedSet<FacetEntry> facets = new TreeSet<>(Collections.reverseOrder());
		// Take the first entry (or entries) in the level map, and build the accumulated entries
		Iterator<Integer> levelIter = levelMap.keySet().iterator();
		if (levelIter.hasNext()) {
			Integer level = levelIter.next();
			LOGGER.debug("Building AccumulatedFacetEntry at level {}", level);
			for (OntologyEntryBean anno : levelMap.get(level)) {
				FacetEntry fe = buildAccumulatedEntryTree(level, anno, entryMap, annotationMap);
				facets.add(fe);
			}
		}
		
		return new ArrayList<FacetEntry>(facets);
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
	
	/**
	 * Convenience method to create a URI-keyed map of facet entries.
	 * @param entries
	 * @return a map of URI : FacetEntry.
	 */
	private Map<String, FacetEntry> convertEntriesToMap(List<FacetEntry> entries) {
		Map<String, FacetEntry> entryMap = new HashMap<>();
		
		for (FacetEntry entry : entries) {
			entryMap.put(entry.getLabel(), entry);
		}
		
		return entryMap;
	}
	
	/**
	 * Fetch the EFO annotations for a list of facet entries.
	 * @param entries
	 * @return a map of URI -> ontology entry for the facet entries.
	 */
	private Map<String, OntologyEntryBean> lookupOntologyEntriesByFacetLabel(List<FacetEntry> entries) {
		List<String> uris = new ArrayList<>(entries.size());
		for (FacetEntry entry : entries) {
			uris.add(entry.getLabel());
		}
		return lookupOntologyEntriesByUri(uris);
	}
	
	/**
	 * Fetch the EFO annotations for a collection of URIs.
	 * @param uris
	 * @return a map of URI -> ontology entry for the incoming URIs.
	 */
	private Map<String, OntologyEntryBean> lookupOntologyEntriesByUri(Collection<String> uris) {
		Map<String, OntologyEntryBean> annotationMap = new HashMap<>();
		
		String query = "*:*";
		String filters = buildFilterString(uris);
		
		try {
			ResultsList<OntologyEntryBean> results = ontologySearch.searchOntology(query, Arrays.asList(filters), 0, uris.size());
			for (OntologyEntryBean anno : results.getResults()) {
				annotationMap.put(anno.getUri(), anno);
			}
		} catch (SearchEngineException e) {
			LOGGER.error("Problem getting ontology entries for filter {}: {}", filters, e.getMessage());
		}
		
		return annotationMap;
	}
	
	/**
	 * Build a filter string for a set of URIs.
	 * @param uris
	 * @return a filter string.
	 */
	private String buildFilterString(Collection<String> uris) {
		StringBuilder sb = new StringBuilder(SolrOntologySearch.URI_FIELD).append(":(");
		
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
	 * Get all of the parent URIs for a collection of ontology entries.
	 * @param annotations
	 * @return the parent URIs.
	 */
	private Set<String> extractParentUris(Collection<OntologyEntryBean> annotations) {
		Set<String> parentUris = new HashSet<>();
		
		for (OntologyEntryBean anno : annotations) {
			parentUris.addAll(anno.getAncestorUris());
		}
		
		return parentUris;
	}
	
	private Map<Integer, List<OntologyEntryBean>> collateAnnotationsByLevel(Map<String, OntologyEntryBean> annotationMap) {
		Map<Integer, List<OntologyEntryBean>> levelMap = new TreeMap<>();

		for (OntologyEntryBean anno : annotationMap.values()) {
			int level = anno.getAncestorUris().size();
			if (!levelMap.containsKey(level)) {
				levelMap.put(level, new ArrayList<OntologyEntryBean>());
			}

			levelMap.get(level).add(anno);
		}
		
		return levelMap;
	}
	
}
