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

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.search.SyntaxError;

import uk.co.flax.biosolr.impl.ChildNodeFacetTreeBuilder;
import uk.co.flax.biosolr.impl.ParentNodeFacetTreeBuilder;

/**
 * Factory class to construct an appropriate {@link FacetTreeBuilder}
 * for the given SolrParams.
 * 
 * @author mlp
 */
public class FacetTreeBuilderFactory {
	
	public static final String CHILD_NODE_STRATEGY = "childnode";
	public static final String PARENT_NODE_STRATEGY = "parentnode";
	
	public FacetTreeBuilderFactory() {
	}
	
	/**
	 * Construct a {@link FacetTreeBuilder} from a set of local parameters.
	 * @param params the local parameters parsed from a 
	 * <code>facet.tree.field</code> parameter.
	 * @return an appropriate {@link FacetTreeBuilder}, or <code>null</code>
	 * if none could be found.
	 * @throws SyntaxError if required parameters are missing for the derived
	 * {@link FacetTreeBuilder}, or the strategy is unrecognised.
	 */
	public FacetTreeBuilder constructFacetTreeBuilder(SolrParams params) throws SyntaxError {
		if (params == null) {
			throw new SyntaxError("No local parameters supplied.");
		}
		
		String strategy = deriveStrategyFromLocalParams(params);
		if (StringUtils.isBlank(strategy)) {
			throw new SyntaxError("Unable to derive strategy from parameters");
		}
		
		FacetTreeBuilder ftb = constructFacetTreeBuilderFromStrategy(strategy);
		if (ftb == null) {
			throw new SyntaxError("Unrecognised strategy for facet tree: " + strategy);
		}

		// Initialise the FacetTreeBuilder parameters
		ftb.initialiseParameters(params);
		
		return ftb;
	}
	
	private String deriveStrategyFromLocalParams(SolrParams params) {
		String strategy = params.get(HierarchicalFacets.STRATEGY_PARAM);
		
		if (StringUtils.isBlank(strategy)) {
			// Attempt to derive strategy from given parameters
			if (StringUtils.isNotBlank(params.get(HierarchicalFacets.CHILD_FIELD_PARAM))) {
				strategy = CHILD_NODE_STRATEGY;
			} else if (StringUtils.isNotBlank(params.get(HierarchicalFacets.PARENT_FIELD_PARAM))) {
				strategy = PARENT_NODE_STRATEGY;
			}
		}
		
		return strategy;
	}
	
	private FacetTreeBuilder constructFacetTreeBuilderFromStrategy(String strategy) {
		FacetTreeBuilder ftb = null;
		
		if (StringUtils.isNotBlank(strategy)) {
			if (strategy.equals(CHILD_NODE_STRATEGY)) {
				ftb = new ChildNodeFacetTreeBuilder();
			} else if (strategy.equals(PARENT_NODE_STRATEGY)) {
				ftb = new ParentNodeFacetTreeBuilder();
			}
		}
		
		return ftb;
	}

}
