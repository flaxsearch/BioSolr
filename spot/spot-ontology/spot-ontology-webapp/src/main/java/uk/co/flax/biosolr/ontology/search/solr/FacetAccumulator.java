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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

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
		Map<String, AccumulatedFacetEntry> entryCache = new HashMap<>();
		Map<String, EFOAnnotation> annotationMap = searchFacetEntries(entries);
		Map<String, FacetEntry> entryMap = convertEntriesToMap(entries);
		
		for (FacetEntry entry : entries) {
			String uri = entry.getLabel();
			entryCache.put(uri, buildAccumulatedEntry(uri, entryCache, annotationMap, entryMap));
		}
		
		List<FacetEntry> retList = sortEntries(entryCache);
		
		return retList;
	}
	
	private AccumulatedFacetEntry buildAccumulatedEntry(String uri, Map<String, AccumulatedFacetEntry> accumulatorCache, Map<String, EFOAnnotation> annotationMap, Map<String, FacetEntry> entryMap) {
		AccumulatedFacetEntry afe = null;
		
		if (accumulatorCache.containsKey(uri)) {
			afe = accumulatorCache.get(uri);
		} else {
			// Fetch the annotation details
			EFOAnnotation anno = annotationMap.get(uri);
			List<AccumulatedFacetEntry> childHierarchy = new ArrayList<>();
			long childTotal = 0;
			if (anno.getSubclassUris() != null) {
				for (String subUri : anno.getSubclassUris()) {
					if (annotationMap.containsKey(subUri)) {
						AccumulatedFacetEntry subAfe = buildAccumulatedEntry(subUri, accumulatorCache, annotationMap, entryMap);
						if (subAfe != null) {
							childTotal += subAfe.getCount();
							childHierarchy.add(subAfe);
						}
					}
				}
			}
			FacetEntry fe = entryMap.get(uri);
			afe = new AccumulatedFacetEntry(uri, anno.getLabel().get(0), fe.getCount(), childTotal, childHierarchy);
		}
		
		return afe;
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
		StringBuilder sb = new StringBuilder(SolrOntologySearch.URI_FIELD).append(":(");
		
		int idx = 0;
		for (FacetEntry entry : entries) {
			if (idx > 0) {
				sb.append(" OR ");
			}
			
			sb.append("\"").append(entry.getLabel()).append("\"");
			idx ++;
		}
		sb.append(")");
		
		return sb.toString();
	}
	
	private List<FacetEntry> sortEntries(Map<String, AccumulatedFacetEntry> entries) {
		SortedMap<Long, FacetEntry> sortedEntries = new TreeMap<>(Collections.reverseOrder());
		for (AccumulatedFacetEntry afe : entries.values()) {
			sortedEntries.put(afe.getTotalCount(), afe);
		}
		
		return new ArrayList<>(sortedEntries.values());
	}
	
}
