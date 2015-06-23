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

package uk.co.flax.biosolr.pruning;

import org.apache.solr.common.params.SolrParams;

import uk.co.flax.biosolr.HierarchicalFacets;

/**
 * Factory class for instantiating the appropriate Pruner instance
 * for the given local parameters.
 *
 * @author mlp
 */
public class PrunerFactory {
	
	public Pruner constructPruner(SolrParams params) {
		Pruner pruner = null;
		
		if (params.getBool(HierarchicalFacets.PRUNE_PARAM, false)) {
			pruner = new SimplePruner();
		}
		
		return pruner;
	}

}
