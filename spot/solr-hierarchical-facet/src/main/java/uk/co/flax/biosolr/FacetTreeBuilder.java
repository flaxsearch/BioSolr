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
import java.util.List;
import java.util.Map;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SyntaxError;

/**
 * A processor to build the facet tree from the incoming base
 * facet values, customised for a particular generation strategy.
 * This could mean recursing up through the tree, using a child ID
 * field to find matches, recursing down through the tree using
 * a parent ID field to find matches, or another strategy depending
 * on what data has been indexed in the ontology.
 * 
 * @author mlp
 */
public interface FacetTreeBuilder {
	
	/**
	 * Initialise the parameters required for the builder implementation.
	 * @param localParameters the local parameters passed for this facet tree
	 * build.
	 * @throws SyntaxError if required parameters are missing.
	 */
	void initialiseParameters(SolrParams localParameters) throws SyntaxError;

	/**
	 * Process the terms from the incoming facets, and use them to build a list of nodes
	 * which are then be converted into a hierarchical facet structure.
	 * @param searcher the searcher to use to build the tree.
	 * @param facetMap the incoming facet values.
	 * @return a list of TreeFacetFields, each of which is the root of a hierarchical
	 * node structure.
	 * @throws IOException if problems occur building the tree, such as errors thrown
	 * by Solr while querying the collection.
	 */
	List<TreeFacetField> processFacetTree(SolrIndexSearcher searcher, Map<String, Integer> facetMap) throws IOException;
	
}
