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

import uk.co.flax.biosolr.HierarchicalFacets;
import uk.co.flax.biosolr.TreeFacetField;

/**
 * FacetTreeBuilder implementation that uses parent node IDs to build a
 * tree from the bottom node upwards.
 * 
 * <p>
 * Minimum required parameters for this tree builder are the node field,
 * either passed in local parameters or taken from the key value, and 
 * the parent node field. {@link #initialiseParameters(SolrParams)} will
 * throw a SyntaxError if these values are not defined.
 * </p>
 *
 * @author mlp
 */
public class ParentNodeFacetTreeBuilder extends AbstractFacetTreeBuilder {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ParentNodeFacetTreeBuilder.class);

	private String parentField;
	private int maxLevels;
	
	private final Set<String> docFields = new HashSet<>();
	
	@Override
	public void initialiseParameters(SolrParams localParams) throws SyntaxError {
		// Initialise the common fields
		super.initialiseParameters(localParams);

		// Initialise the parent field - REQUIRED
		parentField = localParams.get(HierarchicalFacets.PARENT_FIELD_PARAM);
		if (StringUtils.isBlank(parentField)) {
			throw new SyntaxError("Missing parent field definition in " + localParams);
		}
		
		//  Initialise the optional fields
		maxLevels = localParams.getInt(HierarchicalFacets.LEVELS_PARAM, 0);
		
		docFields.addAll(Arrays.asList(getNodeField(), parentField));
		if (hasLabelField()) {
			docFields.add(getLabelField());
		}
	}

	@Override
	public List<TreeFacetField> processFacetTree(SolrIndexSearcher searcher, Map<String, Integer> facetMap)
			throws IOException {
		checkFieldsInSchema(searcher, docFields);
		
		// Extract the facet keys to a volatile set
		Set<String> facetKeys = new HashSet<>(facetMap.keySet());

		// Build a map of parent - child node IDs. This should contain the parents
		// of all our starting facet terms.
		Map<String, Set<String>> nodeChildren = findParentEntries(searcher, facetKeys);

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
	 * Find all parent nodes for the given set of items.
	 * @param searcher the searcher for the collection being used.
	 * @param facetValues the starting set of node IDs.
	 * @return a map of nodes, keyed by their IDs.
	 * @throws IOException
	 */
	private Map<String, Set<String>> findParentEntries(SolrIndexSearcher searcher, Collection<String> facetValues)
			throws IOException {
		Map<String, Set<String>> nodeParentIds = new HashMap<>();

		Set<String> nodesFound = new HashSet<>();
		Set<String> nodeIds = new HashSet<>(facetValues);

		int count = 0;
		while (nodeIds.size() > 0 && (maxLevels == 0 || maxLevels >= count)) {
			// Find the direct parents for the current node IDs
			Map<String, Set<String>> parents = findParentIdsForNodes(searcher, nodeIds);
			nodeParentIds.putAll(parents);
			nodesFound.addAll(nodeIds);

			// Get the parent IDs from all the retrieved nodes - these are the next set of
			// nodes whose parents should be found.
			nodeIds = new HashSet<>();
			for (Set<String> parentIds : parents.values()) {
				nodeIds.addAll(parentIds);
			}
			// Strip out any nodes we've already looked up
			nodeIds.removeAll(nodesFound);
			
			count ++;
		};
		
		// Now, invert the map, so it's a map of parent->child IDs
		Map<String, Set<String>> parentChildIds = new HashMap<>();
		for (String childId : nodeParentIds.keySet()) {
			for (String parentId : nodeParentIds.get(childId)) {
				if (!parentChildIds.containsKey(parentId)) {
					parentChildIds.put(parentId, new HashSet<String>());
				}
				parentChildIds.get(parentId).add(childId);
			}
		}

		return parentChildIds;
	}
	
	private Map<String, Set<String>> findParentIdsForNodes(SolrIndexSearcher searcher, Collection<String> nodeIds) throws IOException {
		Map<String, Set<String>> parentIds = new HashMap<>();
		
		LOGGER.debug("Looking up parents for {} nodes", nodeIds.size());
		Query filter = buildFilterQuery(getNodeField(), nodeIds);
		LOGGER.trace("Filter query: {}", filter);
		
		DocSet docs = searcher.getDocSet(filter);
		
		for (DocIterator it = docs.iterator(); it.hasNext(); ) {
			Document doc = searcher.doc(it.nextDoc(), docFields);
			String nodeId = doc.get(getNodeField());
			
			Set<String> parentIdValues = new HashSet<>(Arrays.asList(doc.getValues(parentField)));
			parentIds.put(nodeId, parentIdValues);
			
			// Record the label, if required
			if (isLabelRequired(nodeId)) {
				recordLabel(nodeId, doc.getValues(getLabelField()));
			}
		}
		
		return parentIds;
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
				if (!childId.equals(fieldValue)) {
					// Found a child of this node - recurse to build its facet tree
					LOGGER.trace("[{}] Building child tree for {}, with {} children", level, childId, 
							(hierarchyMap.containsKey(childId) ? hierarchyMap.get(childId).size(): 0));
					TreeFacetField childTree = buildAccumulatedEntryTree(level + 1, childId, hierarchyMap, facetCounts);
					
					// Only add to the total count if this node isn't already in the child hierarchy
					if (childHierarchy.add(childTree)) {
						childTotal += childTree.getTotal();
					}
					LOGGER.trace("[{}] child tree total: {} - child Total {}, child count {}", level, childTree.getTotal(), childTotal, childHierarchy.size());
				} else {
					LOGGER.trace("[{}] found self-referring ID {}->{}", level, fieldValue, childId);
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
	
	@Override
	protected Logger getLogger() {
		return LOGGER;
	}

}
