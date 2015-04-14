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

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.request.SimpleFacets;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetTreeProcessor extends SimpleFacets {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FacetTreeProcessor.class);
	
	public static final String CHILD_FIELD_PARAM = "childField";

	public FacetTreeProcessor(SolrQueryRequest req, DocSet docs, SolrParams params, ResponseBuilder rb) {
		super(req, docs, params, rb);
	}

	public NamedList<Object> process(String[] facetTrees) throws IOException {
		if (!rb.doFacets || facetTrees == null) {
			return null;
		}

		NamedList<Object> treeResponse = new NamedList<>();
		for (String fTree : facetTrees) {
			try {
				// NOTE: this sets localParams (SimpleFacets is stateful)
				this.parseParams(TreeFacetComponent.FACET_TREE, fTree);
				if (localParams.get(CHILD_FIELD_PARAM) == null) {
					throw new SyntaxError("Missing child field definition in " + fTree);
				}
			} catch (SyntaxError e) {
				throw new SolrException(ErrorCode.BAD_REQUEST, e);
			}
			
			// Verify that the field and child field both exist in the schema
			SolrIndexSearcher searcher = rb.req.getSearcher();
			for (String fieldName : Arrays.asList(key, localParams.get(CHILD_FIELD_PARAM))) {
				SchemaField sField = searcher.getSchema().getField(fieldName);
				if (sField == null) {
					throw new SolrException(ErrorCode.BAD_REQUEST, "\"" + fieldName
							+ "\" is not a valid field name in facet tree: " + fTree);
				}
			}
			
			List<TreeFacetField> fTrees = processFacetTree(key, localParams.get(CHILD_FIELD_PARAM));
			treeResponse.add(key, fTrees);
		}

		return treeResponse;
	}
	
	private List<TreeFacetField> processFacetTree(String field, String childField) throws IOException {
		// Get the initial facet terms for the field
		NamedList<Integer> fieldFacet = getTermCounts(field);
		
		// Convert the term counts to a map (and a set of keys)
		Map<String, Integer> facetMap = new HashMap<>();
		Set<String> facetValues = new HashSet<>(fieldFacet.size());
		for (Iterator<Entry<String, Integer>> it = fieldFacet.iterator(); it.hasNext(); ) {
			Entry<String, Integer> entry = it.next();
			facetMap.put(entry.getKey(), entry.getValue());
			facetValues.add(entry.getKey());
		}
		
		// Get the parent entries
		Map<String, Set<String>> parentEntries = findParentEntries(facetValues, field, childField);
		
		// Find the bottom-level nodes, if there are any which haven't been looked up
		facetValues.removeAll(parentEntries.keySet());
		parentEntries.putAll(filterEntriesByField(facetValues, field, childField));
		
		// Find the top node(s)
		Set<String> topUris = findTopLevelNodes(parentEntries);
		LOGGER.debug("Found {} top level nodes", topUris.size());
		
		List<TreeFacetField> tffs = new ArrayList<>(topUris.size());
		for (String fieldValue : topUris) {
			tffs.add(buildAccumulatedEntryTree(0, fieldValue, parentEntries.get(fieldValue), facetMap, parentEntries));
		}
		
		return tffs;
	}

	/**
	 * Find all parent nodes for the given set of URIs.
	 * @param facetValues the starting set of URIs.
	 * @return a map of nodes, keyed by their URIs.
	 * @throws IOException 
	 */
	private Map<String, Set<String>> findParentEntries(Collection<String> facetValues, String treeField, String filterField) throws IOException {
		Map<String, Set<String>> parentEntries = new HashMap<>();
		
		Set<String> childrenFound = new HashSet<>();
		Set<String> childIds = new HashSet<>(facetValues);
		
		while (childIds.size() > 0) {
			// Find the direct parents for the current child URIs
			Map<String, Set<String>> parents = filterEntriesByField(childIds, treeField, filterField);
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
	 * Fetch the EFO annotations containing one or more URIs in a particular field.
	 * @param uris the URIs to check for.
	 * @param uriField the field to filter against.
	 * @return a map of URI to ontology entry for the incoming URIs.
	 * @throws IOException 
	 */
	private Map<String, Set<String>> filterEntriesByField(Collection<String> facetValues, String treeField, String filterField) throws IOException {
		Map<String, Set<String>> filteredEntries = new HashMap<>();

		if (facetValues.size() > 0) {
			LOGGER.debug("Looking up {} entries in field {}", facetValues.size(), filterField);
			Query query = new MatchAllDocsQuery();
			Query filter = buildFilterQuery(filterField, facetValues);
			
			SolrIndexSearcher searcher = rb.req.getSearcher();
			DocList docs = searcher.getDocList(query, filter, null, 0, facetValues.size());
			
			for (DocIterator it = docs.iterator(); it.hasNext(); ) {
				int id = it.nextDoc();
				Document doc = searcher.doc(id);
				Set<String> childIds = new HashSet<>(Arrays.asList(doc.getValues(filterField)));
				filteredEntries.put(doc.get(treeField), childIds);
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
		StringBuilder sb = new StringBuilder("(");
		
		for (Iterator<String> it = values.iterator(); it.hasNext(); ) {
			String value = it.next();
			sb.append(value);
			
			if (it.hasNext()) {
				sb.append(" OR ");
			}
		}
		
		sb.append(")");
		
		return new TermQuery(new Term(field, sb.toString()));
	}
	
	/**
	 * Find all of the top-level records in a set of annotations. This is done by ooping
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
	 * @param node the current node.
	 * @param facetCounts the facet counts, keyed by node ID.
	 * @param annotationMap the map of annotations (either in the original facet set,
	 * or parent entries of those entries).
	 * @return an {@link AccumulatedFacetEntry} containing details for the current node and all
	 * sub-nodes down to the lowest leaf which has a facet count.
	 */
	private TreeFacetField buildAccumulatedEntryTree(int level, String fieldValue, Set<String> node,
			Map<String, Integer> facetCounts, Map<String, Set<String>> annotationMap) {
		// Build the child hierarchy for this entry
		SortedSet<TreeFacetField> childHierarchy = new TreeSet<>(Collections.reverseOrder());
		long childTotal = 0;
		if (node != null) {
			// Loop through all the direct child URIs, looking for those which are in the annotation map
			for (String childUri : node) {
				if (annotationMap.containsKey(childUri)) {
					// Found a child of this node - recurse to build its facet tree
					LOGGER.trace("[{}] Building subAfe for {}", level, childUri);
					TreeFacetField subAfe = buildAccumulatedEntryTree(level + 1, childUri, annotationMap.get(childUri),
							facetCounts, annotationMap);
					// childTotal is the total facet hits below the current level
					childTotal += subAfe.getTotal();
					childHierarchy.add(subAfe);
					LOGGER.trace("[{}] subAfe total: {} - child Total {}, child count {}", level, subAfe.getTotal(), childTotal, childHierarchy.size());
				}
			}
		}

		// Get the count and label for this entry
		long count = getFacetCount(fieldValue, facetCounts);
		
		// Build the accumulated facet entry
		LOGGER.trace("[{}] Building AFE for {}", level, fieldValue);
		return new TreeFacetField(fieldValue, count, childTotal, childHierarchy);
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
		}
		
		return ret;
	}
	
	class FacetTreeEntry {
		
		List<String> treeIds;
		Set<String> childIds;
		
	}
	
}
