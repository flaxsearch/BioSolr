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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.pruning.Pruner;

/**
 * Class to generate a facet tree.
 * 
 * @author mlp
 */
public class FacetTreeGenerator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FacetTreeGenerator.class);
	
	private final FacetTreeBuilder treeBuilder;
	private final String collection;
	private final Pruner pruner;
	
	public FacetTreeGenerator(FacetTreeBuilder treeBuilder, String collection, Pruner pruner) {
		this.treeBuilder = treeBuilder;
		this.collection = collection;
		this.pruner = pruner;
	}
	
	public List<SimpleOrderedMap<Object>> generateTree(ResponseBuilder rb, NamedList<Integer> facetValues) throws IOException {
		List<SimpleOrderedMap<Object>> retVal = null;
		
		// First get the searcher for the required collection
		RefCounted<SolrIndexSearcher> searcherRef = getSearcherReference(rb);
		
		try {
			// Build the facet tree(s)
			Collection<TreeFacetField> fTrees = treeBuilder.processFacetTree(searcherRef.get(), extractFacetValues(facetValues));
			LOGGER.debug("Extracted {} facet trees", fTrees.size());
			
			if (pruner != null) {
				// Prune the trees
				fTrees = pruner.prune(fTrees);
			}

			// Convert the trees into a SimpleOrderedMap
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
	 * Convert a list of facets into a map, keyed by the facet term. 
	 * @param facetValues the facet values.
	 * @return a map of term - value for each entry.
	 */
	private Map<String, Integer> extractFacetValues(NamedList<Integer> facetValues) {
		Map<String, Integer> facetMap = new LinkedHashMap<>();
		for (Iterator<Entry<String, Integer>> it = facetValues.iterator(); it.hasNext(); ) {
			Entry<String, Integer> entry = it.next();
			if (entry.getValue() > 0) {
				facetMap.put(entry.getKey(), entry.getValue());
			}
		}
		
		return facetMap;
	}
	

	/**
	 * Convert the tree facet fields into a list of SimpleOrderedMaps, so they can
	 * be easily serialized by Solr.
	 * @param fTrees the list of facet tree fields.
	 * @return a list of equivalent maps.
	 */
	private List<SimpleOrderedMap<Object>> convertTreeFacetFields(Collection<TreeFacetField> fTrees) {
		return fTrees.stream().map(TreeFacetField::toMap).collect(Collectors.toList());
	}

}
