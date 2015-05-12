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
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.request.SimpleFacets;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocListAndSet;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.util.RefCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetTreeProcessor extends SimpleFacets {

	private static final Logger LOGGER = LoggerFactory.getLogger(FacetTreeProcessor.class);

	public static final String LOCAL_PARAM_TYPE = "ftree";
	public static final String CHILD_FIELD_PARAM = "childField";
	public static final String COLLECTION_PARAM = "collection";
	public static final String NODE_FIELD_PARAM = "nodeField";
	public static final String LABEL_FIELD_PARAM = "labelField";

	private String nodeField;
	private String childField;
	private String labelField;
	private Map<String, String> labels = new HashMap<>();
	
	private RefCounted<SolrIndexSearcher> searcherRef;

	public FacetTreeProcessor(SolrQueryRequest req, DocSet docs, SolrParams params, ResponseBuilder rb) {
		super(req, docs, params, rb);
	}

	public SimpleOrderedMap<List<SimpleOrderedMap<Object>>> process(String[] facetTrees) throws IOException {
		if (!rb.doFacets || facetTrees == null || facetTrees.length == 0) {
			return null;
		}

		SimpleOrderedMap<List<SimpleOrderedMap<Object>>> treeResponse = new SimpleOrderedMap<>();
		for (String fTree : facetTrees) {
			try {
				// NOTE: this sets localParams (SimpleFacets is stateful)
				this.parseParams(LOCAL_PARAM_TYPE, fTree);
				if (localParams == null) {
					throw new SyntaxError("Missing facet tree parameters");
				} else if (localParams.get(CHILD_FIELD_PARAM) == null) {
					throw new SyntaxError("Missing child field definition in " + fTree);
				} else if (localParams.get(NODE_FIELD_PARAM) == null) {
					nodeField = key;
				} else {
					nodeField = localParams.get(NODE_FIELD_PARAM);
				}
				childField = localParams.get(CHILD_FIELD_PARAM);
				labelField = localParams.get(LABEL_FIELD_PARAM);
			} catch (SyntaxError e) {
				throw new SolrException(ErrorCode.BAD_REQUEST, e);
			}

			// Verify that the field and child field both exist in the schema
			SolrIndexSearcher searcher = rb.req.getSearcher();
			if (StringUtils.isNotBlank(localParams.get(COLLECTION_PARAM))) {
				// Get the SolrCore
				SolrCore currentCore = rb.req.getCore();
				CoreDescriptor cd = currentCore.getCoreDescriptor();
				SolrCore reqCore = cd.getCoreContainer().getCore(localParams.get(COLLECTION_PARAM));
				searcherRef = reqCore.getSearcher();
				searcher = searcherRef.get();
			}

			// Check that all of the fields are in the schema
			for (String fieldName : Arrays.asList(nodeField, childField)) {
				SchemaField sField = searcher.getSchema().getField(fieldName);
				if (sField == null) {
					throw new SolrException(ErrorCode.BAD_REQUEST, "\"" + fieldName
							+ "\" is not a valid field name in facet tree: " + fTree);
				}
			}
			// Check the (optional) label field
			if (labelField != null) {
				if (searcher.getSchema().getField(labelField) == null) {
					throw new SolrException(ErrorCode.BAD_REQUEST, "\"" + labelField
						+ "\" is not a valid field name in facet tree: " + fTree);
				}
			}

			List<TreeFacetField> fTrees = processFacetTree(searcher, key, nodeField, childField);
			if (searcherRef != null) {
				searcherRef.decref();
			}

			treeResponse.add(key, convertTreeFacetFields(fTrees));
		}

		return treeResponse;
	}

	private List<SimpleOrderedMap<Object>> convertTreeFacetFields(List<TreeFacetField> fTrees) {
		List<SimpleOrderedMap<Object>> nlTrees = new ArrayList<>(fTrees.size());
		for (TreeFacetField tff : fTrees) {
			nlTrees.add(tff.toMap());
		}
		return nlTrees;
	}

	private List<TreeFacetField> processFacetTree(SolrIndexSearcher searcher, String facetField, String field, String childField) throws IOException {
		// Get the initial facet terms for the field
		NamedList<Integer> fieldFacet = getTermCounts(facetField);

		// Convert the term counts to a map (and a set of keys)
		Map<String, Integer> facetMap = new HashMap<>();
		Set<String> facetValues = new HashSet<>(fieldFacet.size());
		for (Iterator<Entry<String, Integer>> it = fieldFacet.iterator(); it.hasNext(); ) {
			Entry<String, Integer> entry = it.next();
			if (entry.getValue() > 0) {
				facetMap.put(entry.getKey(), entry.getValue());
				facetValues.add(entry.getKey());
			}
		}

		// Get the parent entries
		Map<String, Set<String>> parentEntries = findParentEntries(searcher, facetValues, field, childField);

		// Find the bottom-level nodes, if there are any which haven't been looked up
		facetValues.removeAll(parentEntries.keySet());
		parentEntries.putAll(filterEntriesByField(searcher, facetValues, field, field));

		// Find the top node(s)
		Set<String> topUris = findTopLevelNodes(parentEntries);
		LOGGER.debug("Found {} top level nodes", topUris.size());

		List<TreeFacetField> tffs = new ArrayList<>(topUris.size());
		for (String fieldValue : topUris) {
			tffs.add(buildAccumulatedEntryTree(0, fieldValue, parentEntries, facetMap));
		}

		return tffs;
	}

	/**
	 * Find all parent nodes for the given set of items.
	 * @param searcher the searcher for the collection being used.
	 * @param facetValues the starting set of node IDs.
	 * @param treeField the item field containing the node value.
	 * @param filterField the item field containing the child values.
	 * @return a map of nodes, keyed by their URIs.
	 * @throws IOException
	 */
	private Map<String, Set<String>> findParentEntries(SolrIndexSearcher searcher, Collection<String> facetValues,
			String treeField, String filterField) throws IOException {
		Map<String, Set<String>> parentEntries = new HashMap<>();

		Set<String> childrenFound = new HashSet<>();
		Set<String> childIds = new HashSet<>(facetValues);

		while (childIds.size() > 0) {
			// Find the direct parents for the current child URIs
			Map<String, Set<String>> parents = filterEntriesByField(searcher, childIds, treeField, filterField);
			parentEntries.putAll(parents);
			childrenFound.addAll(childIds);

			// Get the IDs for all the retrieved nodes - these are the next set of
			// nodes whose parents should be found.
			childIds = parents.keySet();
			// Strip out any nodes we've already looked up
			childIds.removeAll(childrenFound);
		};

		return parentEntries;
	}

	/**
	 * Fetch facets for items containing a specific set of values.
	 * @param searcher the searcher for the collection being used.
	 * @param facetValues the incoming values to use as filters.
	 * @param treeField the item field containing the node value.
	 * @param filterField the item field containing the child values, which will be used
	 * to filter against.
	 * @return a map of node value to child values for the items.
	 * @throws IOException
	 */
	private Map<String, Set<String>> filterEntriesByField(SolrIndexSearcher searcher, Collection<String> facetValues,
			String treeField, String filterField) throws IOException {
		Map<String, Set<String>> filteredEntries = new HashMap<>();

		if (facetValues.size() > 0) {
			LOGGER.debug("Looking up {} entries in field {}", facetValues.size(), filterField);
			Query query = new MatchAllDocsQuery(); // buildFilterQuery(filterField, facetValues);
			Query filter = buildFilterQuery(filterField, facetValues);
			LOGGER.trace("Filter query: {}", filter);
			
			int start = 0;
			int len = facetValues.size();
			boolean done = false;
			
			while (!done) {
				DocListAndSet docs = searcher.getDocListAndSet(query, filter, Sort.RELEVANCE, start, len);
				if (docs.docList.matches() > start + len) {
					start = len;
					len = docs.docList.matches() - len;
				} else {
					done = true;
				}

				for (DocIterator it = docs.docList.iterator(); it.hasNext(); ) {
					int id = it.nextDoc();
					Document doc = searcher.doc(id);
					Set<String> childIds = new HashSet<>(Arrays.asList(doc.getValues(filterField)));
					filteredEntries.put(doc.get(treeField), childIds);
					LOGGER.trace("Got {} children for node {}", childIds.size(), doc.get(treeField));
					// If a label field has been specified, get the first available value
					if (labelField != null) {
						String[] labelValues = doc.getValues(labelField);
						if (labelValues.length > 0) {
							labels.put(doc.get(treeField), labelValues[0]);
						}
					}
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
		BooleanQuery bf = new BooleanQuery();

		for (String value : values) {
			Term term = new Term(field, value);
			bf.add(new TermQuery(term), Occur.SHOULD);
		}

		return bf;
	}

	/**
	 * Find all of the top-level records in a set of annotations. This is done by looping
	 * through the annotations and finding any other annotations in the set which
	 * contain the current annotation in their child list.
	 * @param annotations a map of annotation ID to annotation entries to check over.
	 * @return a set containing the identifiers for all of the top-level
	 * annotations found.
	 */
	private Set<String> findTopLevelNodes(Map<String, Set<String>> annotations) {
		Set<String> topLevel = new HashSet<>();

		for (String uri : annotations.keySet()) {
			boolean found = false;

			// Check each annotation in the set to see if this
			// URI is in their child list
			for (Set<String> anno : annotations.values()) {
				if (anno != null && anno.contains(uri)) {
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
	 * @param fieldValue the current node value.
	 * @param childValues the children of the current node.
	 * @param facetCounts the facet counts, keyed by node ID.
	 * @param annotationMap the map of nodes (either in the original facet set,
	 * or parents of those entries).
	 * @return an {@link AccumulatedFacetEntry} containing details for the current node and all
	 * sub-nodes down to the lowest leaf which has a facet count.
	 */
	private TreeFacetField buildAccumulatedEntryTree(int level, String fieldValue, Map<String, Set<String>> annotationMap,
			Map<String, Integer> facetCounts) {
		// Build the child hierarchy for this entry
		SortedSet<TreeFacetField> childHierarchy = new TreeSet<>(Collections.reverseOrder());
		long childTotal = 0;
		if (annotationMap.containsKey(fieldValue)) {
			Set<String> childValues = annotationMap.get(fieldValue);

			// Loop through all the direct child URIs, looking for those which are in the annotation map
			for (String childId : childValues) {
				if (annotationMap.containsKey(childId) && !childId.equals(fieldValue)) {
					// Found a child of this node - recurse to build its facet tree
					LOGGER.trace("[{}] Building subAfe for {}, with {} children", level, childId, annotationMap.get(childId).size());
					TreeFacetField subAfe = buildAccumulatedEntryTree(level + 1, childId, annotationMap, facetCounts);
					if (childHierarchy.add(subAfe)) {
						// childTotal is the total facet hits below the current level
						childTotal += subAfe.getTotal();
					}
					LOGGER.trace("[{}] subAfe total: {} - child Total {}, child count {}", level, subAfe.getTotal(), childTotal, childHierarchy.size());
				} else {
					LOGGER.trace("[{}] no node entry for {}->{}", level, fieldValue, childId);
				}
			}
		}

		// Get the count and label for this entry
		long count = getFacetCount(fieldValue, facetCounts);
		String label = null;
		if (labelField != null) {
			label = labels.get(fieldValue);
		}

		// Build the accumulated facet entry
		LOGGER.trace("[{}] Building AFE for {}", level, fieldValue);
		return new TreeFacetField(label, fieldValue, count, childTotal, childHierarchy);
	}

	/**
	 * Get the count for the facet with the given key.
	 * @param key the key to look up.
	 * @param facetCounts the map of facet counts.
	 * @return the count, or <code>0</code> if the key does not exist in the map.
	 */
	private long getFacetCount(String key, Map<String, Integer> facetCounts) {
		long ret = 0;

		if (facetCounts.containsKey(key)) {
			ret = facetCounts.get(key);
			LOGGER.trace("Got facet count hit for {}: {}", key, ret);
		}

		return ret;
	}

	class FacetTreeEntry {

		List<String> treeIds;
		Set<String> childIds;

	}

}
