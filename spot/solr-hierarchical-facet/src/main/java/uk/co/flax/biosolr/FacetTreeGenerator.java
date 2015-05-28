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
package uk.co.flax.biosolr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to generate a facet tree.
 */
public class FacetTreeGenerator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FacetTreeGenerator.class);
	
	private final String collection;
	private final String nodeField;
	private final String childField;
	private final String labelField;
	private final int maxLevels;
	
	private final Set<String> docFields;
	
	private Map<String, String> labels = new HashMap<>();
	
	public FacetTreeGenerator(String collection, String nodeField, String childField, String labelField, int maxLevels) {
		this.collection = collection;
		this.nodeField = nodeField;
		this.childField = childField;
		this.labelField = labelField;
		this.maxLevels = maxLevels;
		
		docFields = new HashSet<String>(Arrays.asList(nodeField, childField));
		if (labelField != null && StringUtils.isNotBlank(labelField)) {
			docFields.add(labelField);
		}
	}
	
	
	public List<SimpleOrderedMap<Object>> generateTree(ResponseBuilder rb, NamedList<Integer> facetValues) throws IOException {
		List<SimpleOrderedMap<Object>> retVal = null;
		
		// First get the searcher for the required collection
		RefCounted<SolrIndexSearcher> searcherRef = getSearcherReference(rb);
		
		try {
			// Make sure all the fields are in the searcher's schema
			validateFields(searcherRef.get());

			List<TreeFacetField> fTrees = processFacetTree(searcherRef.get(), extractFacetValues(facetValues));

			retVal = convertTreeFacetFields(fTrees);
		} finally {
			// Make sure the search ref count is decreased
			searcherRef.decref();
		}
		
		return retVal;
	}
	
	/**
	 * Get a reference to the searcher for the required collection. If the collection is
	 * not the same as the search collection, we assume it is under the same Solr instance.
	 * @param rb the response builder holding the facets.
	 * @return a counted reference to the searcher.
	 * @throws SolrException if the collection cannot be found.
	 */
	private RefCounted<SolrIndexSearcher> getSearcherReference(ResponseBuilder rb) throws SolrException {
		RefCounted<SolrIndexSearcher> searcherRef;
		
		SolrCore currentCore = rb.req.getCore();
		if (StringUtils.isBlank(collection)) {
			searcherRef = currentCore.getSearcher();
		} else {
			// Using an alternative core - find it
			SolrCore reqCore = currentCore.getCoreDescriptor().getCoreContainer().getCore(collection);
			if (reqCore == null) {
				throw new SolrException(ErrorCode.BAD_REQUEST, "Collection \"" + collection
						+ "\" cannot be found");
			}
			searcherRef = reqCore.getSearcher();
		}
		
		return searcherRef;
	}
	
	/**
	 * Ensure that all of the required fields exist in the searcher's schema.
	 * @param searcher the searcher being used to generate the facet trees.
	 * @throws SolrException if any of the fields do not exist in the schema.
	 */
	private void validateFields(SolrIndexSearcher searcher) throws SolrException {
		// Check that all of the fields are in the schema
		for (String fieldName : Arrays.asList(nodeField, childField)) {
			SchemaField sField = searcher.getSchema().getField(fieldName);
			if (sField == null) {
				throw new SolrException(ErrorCode.BAD_REQUEST, "\"" + fieldName
						+ "\" is not a valid field name");
			}
		}
		// Check the (optional) label field
		if (labelField != null) {
			if (searcher.getSchema().getField(labelField) == null) {
				throw new SolrException(ErrorCode.BAD_REQUEST, "\"" + labelField
					+ "\" is not a valid field name");
			}
		}
	}
	
	/**
	 * Process the terms from the incoming facets, and use them to build a list of nodes
	 * which are then be converted into a hierarchical facet structure. This does multiple
	 * additional searches to find the parent entries for each of the nodes.
	 * @param searcher the searcher to use to build the tree.
	 * @param facetMap the incoming facet values.
	 * @return a list of TreeFacetFields, each of which is the root of a hierarchical
	 * node structure.
	 * @throws IOException
	 */
	private List<TreeFacetField> processFacetTree(SolrIndexSearcher searcher, Map<String, Integer> facetMap) throws IOException {
		// Extract the facet keys to a volatile set
		Set<String> facetKeys = new HashSet<>(facetMap.keySet());

		// Build a map of parent - child node IDs. This should contain the parents
		// of all our starting facet terms.
		Map<String, Set<String>> nodeChildren = findParentEntries(searcher, facetKeys);

		// Find the details for the starting facet terms, if there are any which haven't 
		// been found already.
		facetKeys.removeAll(nodeChildren.keySet());
		nodeChildren.putAll(filterEntriesByField(searcher, facetKeys, nodeField));

		// Find the top nodes
		Set<String> topUris = findTopLevelNodes(nodeChildren);
		LOGGER.debug("Found {} top level nodes", topUris.size());

		List<TreeFacetField> tffs = new ArrayList<>(topUris.size());
		for (String fieldValue : topUris) {
			tffs.add(buildAccumulatedEntryTree(0, fieldValue, nodeChildren, facetMap));
		}

		return tffs;
	}
	
	/**
	 * Convert a list of facets into a map, keyed by the facet term. 
	 * @param facetValues the facet values.
	 * @return a map of term - value for each entry.
	 */
	private Map<String, Integer> extractFacetValues(NamedList<Integer> facetValues) {
		Map<String, Integer> facetMap = new HashMap<>();
		for (Iterator<Entry<String, Integer>> it = facetValues.iterator(); it.hasNext(); ) {
			Entry<String, Integer> entry = it.next();
			if (entry.getValue() > 0) {
				facetMap.put(entry.getKey(), entry.getValue());
			}
		}
		
		return facetMap;
	}
	
	/**
	 * Find all parent nodes for the given set of items.
	 * @param searcher the searcher for the collection being used.
	 * @param facetValues the starting set of node IDs.
	 * @param childField the item field containing the child values.
	 * @return a map of nodes, keyed by their IDs.
	 * @throws IOException
	 */
	private Map<String, Set<String>> findParentEntries(SolrIndexSearcher searcher, Collection<String> facetValues)
			throws IOException {
		Map<String, Set<String>> parentEntries = new HashMap<>();

		Set<String> childrenFound = new HashSet<>();
		Set<String> childIds = new HashSet<>(facetValues);

		int count = 0;
		while (childIds.size() > 0 && (maxLevels == 0 || maxLevels >= count)) {
			// Find the direct parents for the current child IDs
			Map<String, Set<String>> parents = filterEntriesByField(searcher, childIds, childField);
			parentEntries.putAll(parents);
			childrenFound.addAll(childIds);

			// Get the IDs for all the retrieved nodes - these are the next set of
			// nodes whose parents should be found.
			childIds = parents.keySet();
			// Strip out any nodes we've already looked up
			childIds.removeAll(childrenFound);
			
			count ++;
		};

		return parentEntries;
	}

	/**
	 * Fetch facets for items containing a specific set of values.
	 * @param searcher the searcher for the collection being used.
	 * @param facetValues the incoming values to use as filters.
	 * @param filterField the item field containing the child values, which will be used
	 * to filter against.
	 * @return a map of node value to child values for the items.
	 * @throws IOException
	 */
	private Map<String, Set<String>> filterEntriesByField(SolrIndexSearcher searcher, Collection<String> facetValues,
			String filterField) throws IOException {
		Map<String, Set<String>> filteredEntries = new HashMap<>();

		LOGGER.debug("Looking up {} entries in field {}", facetValues.size(), filterField);
		Query filter = buildFilterQuery(filterField, facetValues);
		LOGGER.trace("Filter query: {}", filter);

		DocSet docs = searcher.getDocSet(filter);

		for (DocIterator it = docs.iterator(); it.hasNext(); ) {
			Document doc = searcher.doc(it.nextDoc(), docFields);
			String nodeId = doc.get(nodeField);
			
			// Get the children for the node, if necessary
			Set<String> childIds;
			if (filterField.equals(nodeField)) {
				// Filtering on the node field - child IDs are redundant
				childIds = Collections.emptySet();
			} else {
				childIds = new HashSet<>(Arrays.asList(doc.getValues(filterField)));
				LOGGER.trace("Got {} children for node {}", childIds.size(), nodeId);
			}
			filteredEntries.put(nodeId, childIds);
			
			// If a label field has been specified, get the first available value
			if (labelField != null && !labels.containsKey(nodeId)) {
				String[] labelValues = doc.getValues(labelField);
				if (labelValues.length > 0) {
					labels.put(nodeId, labelValues[0]);
				} else {
					labels.put(nodeId, null);
				}
			}
		}

		return filteredEntries;
	}

	/**
	 * Build a filter query for a field using a set of values, taken from the keys
	 * of a {@link NamedList}.
	 * @param field
	 * @param values
	 * @return a filter string.
	 */
	private Query buildFilterQuery(String field, Collection<String> values) {
		BooleanQuery bf = new BooleanQuery(true);

		for (String value : values) {
			Term term = new Term(field, value);
			bf.add(new TermQuery(term), Occur.SHOULD);
		}

		return bf;
	}

	/**
	 * Find all of the top-level nodes in a map of parent - child node IDs.
	 * @param nodeChildren a map of parent - child node IDs..
	 * @return a set containing the IDs for all of the top-level nodes found.
	 */
	private Set<String> findTopLevelNodes(Map<String, Set<String>> nodeChildren) {
		Set<String> topLevel = new HashSet<>();
		
		// Extract all the child IDs so we only have to iterate through them once
		Set<String> childIds = extractAllChildIds(nodeChildren.values());

		// Loop through each ID in the map, and check if it is contained in the
		// children of any other node.
		for (String id : nodeChildren.keySet()) {
			if (!childIds.contains(id)) {
				// Not in the set of child IDs - must be top level
				topLevel.add(id);
			}
		}

		return topLevel;
	}
	
	/**
	 * Extract all of the entries in the collected child IDs into a single set.
	 * @param childIds the collection of all child ID sets.
	 * @return a single set containing all of the child IDs.
	 */
	private Set<String> extractAllChildIds(Collection<Set<String>> childIds) {
		Set<String> ids = new HashSet<>();
		
		for (Set<String> children : childIds) {
			ids.addAll(children);
		}
		
		return ids;
	}
	
	/**
	 * Recursively build an accumulated facet entry tree.
	 * @param level current level in the tree (used for debugging/logging).
	 * @param fieldValue the current node value.
	 * @param hierarchyMap the map of nodes (either in the original facet set,
	 * or parents of those entries).
	 * @param facetCounts the facet counts, keyed by node ID.
	 * @return a {@link TreeFacetField} containing details for the current node and all
	 * sub-nodes down to the lowest leaf which has a facet count.
	 */
	private TreeFacetField buildAccumulatedEntryTree(int level, String fieldValue, Map<String, Set<String>> hierarchyMap,
			Map<String, Integer> facetCounts) {
		// Build the child hierarchy for this entry.
		// We use a reverse-ordered SortedSet so entries are returned in descending
		// order by their total count.
		SortedSet<TreeFacetField> childHierarchy = new TreeSet<>(Collections.reverseOrder());
		
		// childTotal is the total number of facet hits below this node
		long childTotal = 0;
		if (hierarchyMap.containsKey(fieldValue)) {
			// Loop through all the direct child URIs, looking for those which are in the annotation map
			for (String childId : hierarchyMap.get(fieldValue)) {
				if (hierarchyMap.containsKey(childId) && !childId.equals(fieldValue)) {
					// Found a child of this node - recurse to build its facet tree
					LOGGER.trace("[{}] Building child tree for {}, with {} children", level, childId, hierarchyMap.get(childId).size());
					TreeFacetField childTree = buildAccumulatedEntryTree(level + 1, childId, hierarchyMap, facetCounts);
					
					// Only add to the total count if this node isn't already in the child hierarchy
					if (childHierarchy.add(childTree)) {
						childTotal += childTree.getTotal();
					}
					LOGGER.trace("[{}] child tree total: {} - child Total {}, child count {}", level, childTree.getTotal(), childTotal, childHierarchy.size());
				} else {
					LOGGER.trace("[{}] no node entry for {}->{}", level, fieldValue, childId);
				}
			}
		}

		// Build the accumulated facet entry
		LOGGER.trace("[{}] Building facet tree for {}", level, fieldValue);
		return new TreeFacetField(getFacetLabel(fieldValue), fieldValue, getFacetCount(fieldValue, facetCounts), childTotal, childHierarchy);
	}

	/**
	 * Get the count for the facet with the given key.
	 * @param key the key to look up.
	 * @param facetCounts the map of facet counts.
	 * @return the count, or <code>0</code> if the key does not exist in the map.
	 */
	private long getFacetCount(String key, Map<String, Integer> facetCounts) {
		if (facetCounts.containsKey(key)) {
			return facetCounts.get(key);
		}
		return 0;
	}
	
	private String getFacetLabel(String id) {
		if (labelField != null) {
			return labels.get(id);
		}
		return null;
	}

	/**
	 * Convert the tree facet fields into a list of SimpleOrderedMaps, so they can
	 * be easily serialized by Solr.
	 * @param fTrees the list of facet tree fields.
	 * @return a list of equivalent maps.
	 */
	private List<SimpleOrderedMap<Object>> convertTreeFacetFields(List<TreeFacetField> fTrees) {
		List<SimpleOrderedMap<Object>> nlTrees = new ArrayList<>(fTrees.size());
		for (TreeFacetField tff : fTrees) {
			nlTrees.add(tff.toMap());
		}
		return nlTrees;
	}

}
