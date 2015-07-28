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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.solr.common.params.SolrParams;
import org.junit.Test;

import uk.co.flax.biosolr.FacetTreeParameters;

/**
 * Unit tests for the PrunerFactory.
 *
 * @author mlp
 */
public class PrunerFactoryTest {
	
	@Test(expected=java.lang.NullPointerException.class)
	public void constructPruner_withNullParams() throws Exception {
		final SolrParams params = null;
		final FacetTreeParameters ftParams = mock(FacetTreeParameters.class);
		
		PrunerFactory factory = new PrunerFactory(ftParams);
		factory.constructPruner(params);
	}

	@Test
	public void constructPruner_withNullPrunerParam() throws Exception {
		final String prunerParam = null;
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.PRUNE_PARAM, null)).thenReturn(prunerParam);
		final FacetTreeParameters ftParams = mock(FacetTreeParameters.class);
		when(ftParams.getArgument(FacetTreeParameters.PRUNE_PARAM)).thenReturn(null);
		
		PrunerFactory factory = new PrunerFactory(ftParams);
		assertNull(factory.constructPruner(params));
		
		verify(params).get(FacetTreeParameters.PRUNE_PARAM, null);
	}

	@Test
	public void constructPruner_withDummyPrunerParam() throws Exception {
		final String prunerParam = "dummy";
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.PRUNE_PARAM, null)).thenReturn(prunerParam);
		final FacetTreeParameters ftParams = mock(FacetTreeParameters.class);
		when(ftParams.getArgument(FacetTreeParameters.PRUNE_PARAM)).thenReturn(null);
		
		PrunerFactory factory = new PrunerFactory(ftParams);
		assertNull(factory.constructPruner(params));
		
		verify(params).get(FacetTreeParameters.PRUNE_PARAM, null);
	}

	@Test
	public void constructPruner_simplePruner() throws Exception {
		final String prunerParam = PrunerFactory.SIMPLE_PRUNER_VALUE;
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.PRUNE_PARAM, null)).thenReturn(prunerParam);
		final FacetTreeParameters ftParams = mock(FacetTreeParameters.class);
		when(ftParams.getArgument(FacetTreeParameters.PRUNE_PARAM)).thenReturn(null);
		
		PrunerFactory factory = new PrunerFactory(ftParams);
		Pruner pruner = factory.constructPruner(params);
		assertNotNull(pruner);
		assertTrue(pruner instanceof SimplePruner);
		
		verify(params).get(FacetTreeParameters.PRUNE_PARAM, null);
	}

	@Test(expected=org.apache.solr.search.SyntaxError.class)
	public void constructPruner_datapointsPruner_noDatapoints() throws Exception {
		final String prunerParam = PrunerFactory.DATAPOINTS_PRUNER_VALUE;
		final FacetTreeParameters ftParams = mock(FacetTreeParameters.class);
		when(ftParams.getArgument(FacetTreeParameters.PRUNE_PARAM)).thenReturn(null);
		when(ftParams.getIntArgument(FacetTreeParameters.DATAPOINTS_PARAM)).thenReturn(0);
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.PRUNE_PARAM, null)).thenReturn(prunerParam);
		when(params.getInt(FacetTreeParameters.DATAPOINTS_PARAM, 0)).thenReturn(0);
		
		PrunerFactory factory = new PrunerFactory(ftParams);
		factory.constructPruner(params);
		
		verify(params).get(FacetTreeParameters.PRUNE_PARAM, null);
		verify(ftParams).getIntArgument(FacetTreeParameters.DATAPOINTS_PARAM);
	}

	@Test
	public void constructPruner_datapointsPruner_dpFromDefaults() throws Exception {
		final String prunerParam = PrunerFactory.DATAPOINTS_PRUNER_VALUE;
		final Integer dp = 6;
		final FacetTreeParameters ftParams = mock(FacetTreeParameters.class);
		when(ftParams.getArgument(FacetTreeParameters.PRUNE_PARAM)).thenReturn(null);
		when(ftParams.getIntArgument(FacetTreeParameters.DATAPOINTS_PARAM)).thenReturn(dp);
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.PRUNE_PARAM, null)).thenReturn(prunerParam);
		when(params.getInt(FacetTreeParameters.DATAPOINTS_PARAM, dp)).thenReturn(dp);
		
		PrunerFactory factory = new PrunerFactory(ftParams);
		factory.constructPruner(params);
		
		verify(params).get(FacetTreeParameters.PRUNE_PARAM, null);
		verify(ftParams).getIntArgument(FacetTreeParameters.DATAPOINTS_PARAM);
	}

	@Test
	public void constructPruner_datapointsPruner_withDatapoints() throws Exception {
		final String prunerParam = PrunerFactory.DATAPOINTS_PRUNER_VALUE;
		final FacetTreeParameters ftParams = mock(FacetTreeParameters.class);
		when(ftParams.getArgument(FacetTreeParameters.PRUNE_PARAM)).thenReturn(null);
		when(ftParams.getIntArgument(FacetTreeParameters.DATAPOINTS_PARAM)).thenReturn(0);
		final Integer dp = 6;
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.PRUNE_PARAM, null)).thenReturn(prunerParam);
		when(params.getInt(FacetTreeParameters.DATAPOINTS_PARAM, 0)).thenReturn(dp);
		
		PrunerFactory factory = new PrunerFactory(ftParams);
		Pruner pruner = factory.constructPruner(params);
		assertNotNull(pruner);
		assertTrue(pruner instanceof DatapointPruner);
		
		verify(params).get(FacetTreeParameters.PRUNE_PARAM, null);
		verify(params).getInt(FacetTreeParameters.DATAPOINTS_PARAM, 0);
		verify(ftParams).getIntArgument(FacetTreeParameters.DATAPOINTS_PARAM);
	}

}
