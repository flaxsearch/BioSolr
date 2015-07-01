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

package uk.co.flax.biosolr.impl;

import java.io.IOException;
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
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.FacetTreeBuilder;
import uk.co.flax.biosolr.HierarchicalFacets;
import uk.co.flax.biosolr.TreeFacetField;

/**
 * Implementation of {@link FacetTreeBuilder} that uses a child node field
 * to build a hierarchical facet tree from the bottom upwards.
 * 
 * <p>
 * Minimum required parameters for this tree builder are the node field,
 * either passed in local parameters or taken from the key value, and 
 * the child node field. {@link #initialiseParameters(SolrParams)} will
 * throw a SyntaxError if these values are not defined.
 * </p>
 */
public class ChildNodeFacetTreeBuilder extends AbstractFacetTreeBuilder {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ChildNodeFacetTreeBuilder.class);
	
	private String childField;
	private int maxLevels;
	
	private final Set<String> docFields = new HashSet<>();
	
	public ChildNodeFacetTreeBuilder() {
	}
	
	@Override
	protected Logger getLogger() {
		return LOGGER;
	}

	@Override
	public void initialiseParameters(SolrParams localParams) throws SyntaxError {
		super.initialiseParameters(localParams);

		// Initialise the child field - REQUIRED
		childField = localParams.get(HierarchicalFacets.CHILD_FIELD_PARAM);
		if (StringUtils.isBlank(childField)) {
			throw new SyntaxError("Missing child field definition in " + localParams);
		}
		
		//  Initialise the optional fields
		maxLevels = localParams.getInt(HierarchicalFacets.LEVELS_PARAM, 0);
		
		docFields.addAll(Arrays.asList(getNodeField(), childField));
		if (hasLabelField()) {
			docFields.add(getLabelField());
		}
	}
	
	@Override
	public List<TreeFacetField> processFacetTree(SolrIndexSearcher searcher, Map<String, Integer> facetMap)
			throws IOException {
		// Check that all of the given fields are in the searcher's schema
		checkFieldsInSchema(searcher, docFields);
		
		// Extract the facet keys to a volatile set
		Set<String> facetKeys = new HashSet<>(facetMap.keySet());

		// Build a map of parent - child node IDs. This should contain the parents
		// of all our starting facet terms.
		Map<String, Set<String>> nodeChildren = findParentEntries(searcher, facetKeys);

		// Find the details for the starting facet terms, if there are any which haven't 
		// been found already.
		facetKeys.removeAll(nodeChildren.keySet());
		nodeChildren.putAll(filterEntriesByField(searcher, facetKeys, getNodeField()));

		// Find the top nodes
		Set<String> topNodes = findTopLevelNodes(nodeChildren);
		LOGGER.debug("Found {} top level nodes", topNodes.size());

		// Convert to a list of TreeFacetFields
		return topNodes.parallelStream()
				.map(node -> buildAccumulatedEntryTree(0, node, nodeChildren, facetMap))
				.collect(Collectors.toList());
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
			String nodeId = doc.get(getNodeField());
			
			// Get the children for the node, if necessary
			Set<String> childIds;
			if (filterField.equals(getNodeField())) {
				// Filtering on the node field - child IDs are redundant
				childIds = Collections.emptySet();
			} else {
				childIds = new HashSet<>(Arrays.asList(doc.getValues(filterField)));
				LOGGER.trace("Got {} children for node {}", childIds.size(), nodeId);
			}
			filteredEntries.put(nodeId, childIds);
			
			// Record the label, if required
			if (isLabelRequired(nodeId)) {
				recordLabel(nodeId, doc.getValues(getLabelField()));
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
		
		values.stream()
			.map(v -> new TermQuery(new Term(field, v)))
			.forEach(tq -> bf.add(tq, Occur.SHOULD));

		return bf;
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
		return new TreeFacetField(getLabel(fieldValue), fieldValue, getFacetCount(fieldValue, facetCounts), childTotal, childHierarchy);
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
	
}
