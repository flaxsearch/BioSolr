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
import uk.co.flax.biosolr.ontology.api.EFOAnnotation;
import uk.co.flax.biosolr.ontology.api.FacetEntry;
import uk.co.flax.biosolr.ontology.search.OntologySearch;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

/**
 * @author Matt Pearce
 */
public class FacetAccumulator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FacetAccumulator.class);
	
	private final OntologySearch ontologySearch;

	public FacetAccumulator(OntologySearch ontologySearch) {
		this.ontologySearch = ontologySearch;
	}
	
	public List<FacetEntry> accumulateEntries(List<FacetEntry> entries) {
		Map<String, EFOAnnotation> annotationMap = searchFacetEntries(entries);
		Map<String, FacetEntry> entryMap = convertEntriesToMap(entries);
		
		Set<String> parentUris = extractParentUris(annotationMap.values());
		// Remove URIs for which we already have EFO annotations
		parentUris.removeAll(annotationMap.keySet());
		Map<String, EFOAnnotation> parentMap = searchParentUris(parentUris);
		
		// Combine the maps
		annotationMap.putAll(parentMap);
		
		// Build a map of annotations by level in the tree, so we can start at the bottom level
		Map<Integer, List<EFOAnnotation>> levelMap = collateAnnotationsByLevel(annotationMap);
		
		SortedSet<FacetEntry> facets = new TreeSet<>(Collections.reverseOrder());
		// Checked set keeps track of which child nodes we've already built
		Set<String> checked = new HashSet<>();
		// Take the first entry (or entries) in the level map, and build the accumulated entries
		Iterator<Integer> levelIter = levelMap.keySet().iterator();
		if (levelIter.hasNext()) {
			Integer level = levelIter.next();
			LOGGER.debug("Building AccumulatedFacetEntry at level {}", level);
			for (EFOAnnotation anno : levelMap.get(level)) {
				FacetEntry fe = buildAccumulatedEntryTree(level, anno, entryMap, annotationMap, checked);
				facets.add(fe);
			}
		}
		
		return new ArrayList<FacetEntry>(facets);
	}
	
	private AccumulatedFacetEntry buildAccumulatedEntryTree(int level, EFOAnnotation node,
			Map<String, FacetEntry> entryMap, Map<String, EFOAnnotation> annotationMap, Set<String> checked) {
		List<AccumulatedFacetEntry> childHierarchy = new ArrayList<>();
		long childTotal = 0;
		if (node.getChildUris() != null) {
			for (String childUri : node.getChildUris()) {
				if (annotationMap.containsKey(childUri)) {
					LOGGER.debug("[{}] Building subAfe for {}", level, childUri);
					AccumulatedFacetEntry subAfe = buildAccumulatedEntryTree(level + 1, annotationMap.get(childUri), entryMap, annotationMap, checked);
					childTotal += subAfe.getTotalCount();
					childHierarchy.add(subAfe);
					LOGGER.debug("[{}] subAfe total: {} - child Total {}, child count {}", level, subAfe.getTotalCount(), childTotal, childHierarchy.size());
				}
			}
		}

		long count = 0;
		if (entryMap.containsKey(node.getUri())) {
			count = entryMap.get(node.getUri()).getCount();
		}
		
		checked.add(node.getUri());

		LOGGER.debug("[{}] Building AFE for {}", level, node.getUri());
		return new AccumulatedFacetEntry(node.getUri(), node.getLabel().get(0), count, childTotal, childHierarchy);
	}
	
	private Map<String, FacetEntry> convertEntriesToMap(List<FacetEntry> entries) {
		Map<String, FacetEntry> entryMap = new HashMap<>();
		
		for (FacetEntry entry : entries) {
			entryMap.put(entry.getLabel(), entry);
		}
		
		return entryMap;
	}
	
	private Map<String, EFOAnnotation> searchFacetEntries(List<FacetEntry> entries) {
		Map<String, EFOAnnotation> annotationMap = new HashMap<>();
		
		String query = "*:*";
		String filters = buildFilterString(entries);
		
		try {
			ResultsList<EFOAnnotation> results = ontologySearch.searchOntology(query, Arrays.asList(filters), 0, entries.size());
			for (EFOAnnotation anno : results.getResults()) {
				annotationMap.put(anno.getUri(), anno);
			}
		} catch (SearchEngineException e) {
			LOGGER.error("Problem getting ontology entries for filter {}: {}", filters, e.getMessage());
		}
		
		return annotationMap;
	}
	
	private String buildFilterString(List<FacetEntry> entries) {
		Set<String> uris = new HashSet<String>();
		for (FacetEntry entry : entries) {
			uris.add(entry.getLabel());
		}
		return buildFilterString(uris);
	}
	
	private String buildFilterString(Set<String> uris) {
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
	
	private Set<String> extractParentUris(Collection<EFOAnnotation> annotations) {
		Set<String> parentUris = new HashSet<>();
		
		for (EFOAnnotation anno : annotations) {
			parentUris.addAll(anno.getAncestorUris());
		}
		
		return parentUris;
	}
	
	private Map<String, EFOAnnotation> searchParentUris(Set<String> parentUris) {
		Map<String, EFOAnnotation> annotationMap = new HashMap<>();
		
		String query = "*:*";
		String filters = buildFilterString(parentUris);
		
		try {
			ResultsList<EFOAnnotation> results = ontologySearch.searchOntology(query, Arrays.asList(filters), 0, parentUris.size());
			for (EFOAnnotation anno : results.getResults()) {
				annotationMap.put(anno.getUri(), anno);
			}
		} catch (SearchEngineException e) {
			LOGGER.error("Problem getting ontology entries for filter {}: {}", filters, e.getMessage());
		}
		
		return annotationMap;
	}
	
	private Map<Integer, List<EFOAnnotation>> collateAnnotationsByLevel(Map<String, EFOAnnotation> annotationMap) {
		Map<Integer, List<EFOAnnotation>> levelMap = new TreeMap<>();

		for (EFOAnnotation anno : annotationMap.values()) {
			if (!levelMap.containsKey(anno.getTreeLevel())) {
				levelMap.put(anno.getTreeLevel(), new ArrayList<EFOAnnotation>());
			}

			levelMap.get(anno.getTreeLevel()).add(anno);
		}
		
		return levelMap;
	}
	
}
