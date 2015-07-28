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

import static uk.co.flax.biosolr.FacetTreeParameters.DATAPOINTS_PARAM;
import static uk.co.flax.biosolr.FacetTreeParameters.PRUNE_PARAM;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.search.SyntaxError;

import uk.co.flax.biosolr.FacetTreeParameters;

/**
 * Factory class for instantiating the appropriate Pruner instance
 * for the given local parameters.
 *
 * @author mlp
 */
public class PrunerFactory {
	
	public static final String SIMPLE_PRUNER_VALUE = "simple";
	public static final String DATAPOINTS_PRUNER_VALUE = "datapoint";
	
	private final FacetTreeParameters componentParameters;
	
	public PrunerFactory(FacetTreeParameters params) {
		this.componentParameters = params;
	}
	
	public Pruner constructPruner(SolrParams params) throws SyntaxError {
		Pruner pruner = null;
		
		String prunerParam = params.get(PRUNE_PARAM, componentParameters.getDefault(PRUNE_PARAM));
		
		if (StringUtils.isNotBlank(prunerParam)) {
			if (SIMPLE_PRUNER_VALUE.equals(prunerParam)) {
				pruner = new SimplePruner(params.getInt(SimplePruner.CHILD_COUNT_PARAM, SimplePruner.MIN_CHILD_COUNT));
			} else if (DATAPOINTS_PRUNER_VALUE.equals(prunerParam)) {
				int dp = params.getInt(DATAPOINTS_PARAM, componentParameters.getIntDefault(DATAPOINTS_PARAM));
				if (dp <= 0) {
					throw new SyntaxError("Datapoints parameter invalid");
				}
				pruner = new DatapointPruner(dp, 
						componentParameters.getDefault(FacetTreeParameters.DATAPOINTS_MORELABEL_PARAM,  DatapointPruner.DEFAULT_MORE_LABEL));
			}
		}
		
		return pruner;
	}

}
