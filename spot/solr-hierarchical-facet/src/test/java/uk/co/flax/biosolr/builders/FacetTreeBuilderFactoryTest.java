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

package uk.co.flax.biosolr.builders;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.solr.common.params.SolrParams;
import org.junit.Test;

import uk.co.flax.biosolr.FacetTreeParameters;

/**
 * Unit tests for the FacetTreeBuilderFactory.
 * 
 * @author mlp
 */
public class FacetTreeBuilderFactoryTest {
	
	@Test(expected=org.apache.solr.search.SyntaxError.class)
	public void constructFTB_nullLocalParams() throws Exception {
		final SolrParams params = null;
		
		FacetTreeBuilderFactory factory = new FacetTreeBuilderFactory();
		factory.constructFacetTreeBuilder(params);
	}

	@Test(expected=org.apache.solr.search.SyntaxError.class)
	public void constructFTB_emptyLocalParams() throws Exception {
		final SolrParams params = mock(SolrParams.class);
		
		FacetTreeBuilderFactory factory = new FacetTreeBuilderFactory();
		factory.constructFacetTreeBuilder(params);
		
		verify(params).get(FacetTreeParameters.STRATEGY_PARAM);
	}

	@Test(expected=org.apache.solr.search.SyntaxError.class)
	public void constructFTB_blankStrategy() throws Exception {
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.STRATEGY_PARAM)).thenReturn("");
		
		FacetTreeBuilderFactory factory = new FacetTreeBuilderFactory();
		factory.constructFacetTreeBuilder(params);
		
		verify(params).get(FacetTreeParameters.STRATEGY_PARAM);
	}

	@Test(expected=org.apache.solr.search.SyntaxError.class)
	public void constructFTB_unrecognisedStrategy() throws Exception {
		final String strategy = "boggle";
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.STRATEGY_PARAM)).thenReturn(strategy);
		
		FacetTreeBuilderFactory factory = new FacetTreeBuilderFactory();
		factory.constructFacetTreeBuilder(params);
		
		verify(params).get(FacetTreeParameters.STRATEGY_PARAM);
	}

	@Test(expected=org.apache.solr.search.SyntaxError.class)
	public void constructFTB_childnodeStrategy_missingParams() throws Exception {
		final String strategy = FacetTreeBuilderFactory.CHILD_NODE_STRATEGY;
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.STRATEGY_PARAM)).thenReturn(strategy);
		
		FacetTreeBuilderFactory factory = new FacetTreeBuilderFactory();
		factory.constructFacetTreeBuilder(params);
		
		verify(params).get(FacetTreeParameters.STRATEGY_PARAM);
	}

	@Test
	public void constructFTB_childnodeStrategy() throws Exception {
		final String strategy = FacetTreeBuilderFactory.CHILD_NODE_STRATEGY;
		final String childField = "child_nodes";
		final String nodeField = "node_id";
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.STRATEGY_PARAM)).thenReturn(strategy);
		when(params.get(FacetTreeParameters.CHILD_FIELD_PARAM)).thenReturn(childField);
		when(params.get(FacetTreeParameters.NODE_FIELD_PARAM)).thenReturn(nodeField);
		
		FacetTreeBuilderFactory factory = new FacetTreeBuilderFactory();
		FacetTreeBuilder ftb = factory.constructFacetTreeBuilder(params);
		
		assertNotNull(ftb);
		assertTrue(ftb instanceof ChildNodeFacetTreeBuilder);
		
		verify(params).get(FacetTreeParameters.STRATEGY_PARAM);
		verify(params, atLeastOnce()).get(FacetTreeParameters.CHILD_FIELD_PARAM);
		verify(params, atLeastOnce()).get(FacetTreeParameters.NODE_FIELD_PARAM);
	}

	@Test(expected=org.apache.solr.search.SyntaxError.class)
	public void constructFTB_derivedChildnodeStrategy_missingParams() throws Exception {
		final String strategy = null;
		final String childField = "child_nodes";
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.STRATEGY_PARAM)).thenReturn(strategy);
		when(params.get(FacetTreeParameters.CHILD_FIELD_PARAM)).thenReturn(childField);
		
		FacetTreeBuilderFactory factory = new FacetTreeBuilderFactory();
		factory.constructFacetTreeBuilder(params);
		
		verify(params).get(FacetTreeParameters.STRATEGY_PARAM);
		verify(params, atLeastOnce()).get(FacetTreeParameters.CHILD_FIELD_PARAM);
	}

	@Test
	public void constructFTB_derivedChildnodeStrategy() throws Exception {
		final String strategy = null;
		final String childField = "child_nodes";
		final String nodeField = "node_id";
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.STRATEGY_PARAM)).thenReturn(strategy);
		when(params.get(FacetTreeParameters.CHILD_FIELD_PARAM)).thenReturn(childField);
		when(params.get(FacetTreeParameters.NODE_FIELD_PARAM)).thenReturn(nodeField);
		
		FacetTreeBuilderFactory factory = new FacetTreeBuilderFactory();
		FacetTreeBuilder ftb = factory.constructFacetTreeBuilder(params);
		
		assertNotNull(ftb);
		assertTrue(ftb instanceof ChildNodeFacetTreeBuilder);
		
		verify(params).get(FacetTreeParameters.STRATEGY_PARAM);
		verify(params, atLeastOnce()).get(FacetTreeParameters.CHILD_FIELD_PARAM);
		verify(params, atLeastOnce()).get(FacetTreeParameters.NODE_FIELD_PARAM);
	}

	@Test(expected=org.apache.solr.search.SyntaxError.class)
	public void constructFTB_parentnodeStrategy_missingParams() throws Exception {
		final String strategy = FacetTreeBuilderFactory.PARENT_NODE_STRATEGY;
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.STRATEGY_PARAM)).thenReturn(strategy);
		
		FacetTreeBuilderFactory factory = new FacetTreeBuilderFactory();
		factory.constructFacetTreeBuilder(params);
		
		verify(params).get(FacetTreeParameters.STRATEGY_PARAM);
	}

	@Test
	public void constructFTB_parentnodeStrategy() throws Exception {
		final String strategy = FacetTreeBuilderFactory.PARENT_NODE_STRATEGY;
		final String parentField = "parent_nodes";
		final String nodeField = "node_id";
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.STRATEGY_PARAM)).thenReturn(strategy);
		when(params.get(FacetTreeParameters.PARENT_FIELD_PARAM)).thenReturn(parentField);
		when(params.get(FacetTreeParameters.NODE_FIELD_PARAM)).thenReturn(nodeField);
		
		FacetTreeBuilderFactory factory = new FacetTreeBuilderFactory();
		FacetTreeBuilder ftb = factory.constructFacetTreeBuilder(params);
		
		assertNotNull(ftb);
		assertTrue(ftb instanceof ParentNodeFacetTreeBuilder);
		
		verify(params).get(FacetTreeParameters.STRATEGY_PARAM);
		verify(params, atLeastOnce()).get(FacetTreeParameters.PARENT_FIELD_PARAM);
		verify(params, atLeastOnce()).get(FacetTreeParameters.NODE_FIELD_PARAM);
	}

	@Test(expected=org.apache.solr.search.SyntaxError.class)
	public void constructFTB_derivedParentnodeStrategy_missingParams() throws Exception {
		final String strategy = null;
		final String parentField = "parent_nodes";
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.STRATEGY_PARAM)).thenReturn(strategy);
		when(params.get(FacetTreeParameters.PARENT_FIELD_PARAM)).thenReturn(parentField);
		
		FacetTreeBuilderFactory factory = new FacetTreeBuilderFactory();
		factory.constructFacetTreeBuilder(params);
		
		verify(params).get(FacetTreeParameters.STRATEGY_PARAM);
		verify(params, atLeastOnce()).get(FacetTreeParameters.PARENT_FIELD_PARAM);
	}

	@Test
	public void constructFTB_derivedParentnodeStrategy() throws Exception {
		final String strategy = null;
		final String parentField = "parent_nodes";
		final String nodeField = "node_id";
		final SolrParams params = mock(SolrParams.class);
		when(params.get(FacetTreeParameters.STRATEGY_PARAM)).thenReturn(strategy);
		when(params.get(FacetTreeParameters.PARENT_FIELD_PARAM)).thenReturn(parentField);
		when(params.get(FacetTreeParameters.NODE_FIELD_PARAM)).thenReturn(nodeField);
		
		FacetTreeBuilderFactory factory = new FacetTreeBuilderFactory();
		FacetTreeBuilder ftb = factory.constructFacetTreeBuilder(params);
		
		assertNotNull(ftb);
		assertTrue(ftb instanceof ParentNodeFacetTreeBuilder);
		
		verify(params).get(FacetTreeParameters.STRATEGY_PARAM);
		verify(params, atLeastOnce()).get(FacetTreeParameters.PARENT_FIELD_PARAM);
		verify(params, atLeastOnce()).get(FacetTreeParameters.NODE_FIELD_PARAM);
	}

}
