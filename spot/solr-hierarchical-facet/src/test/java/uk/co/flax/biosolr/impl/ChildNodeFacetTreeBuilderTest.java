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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.search.QueryParsing;
import org.junit.Test;

import uk.co.flax.biosolr.HierarchicalFacets;

/**
 * Unit tests for ChildNodeFacetTreeBuilder.
 *
 * @author mlp
 */
public class ChildNodeFacetTreeBuilderTest {
	
	@Test(expected=org.apache.solr.search.SyntaxError.class)
	public void initialiseParameters_nullParams() throws Exception {
		final SolrParams params = null;
		
		ChildNodeFacetTreeBuilder ftb = new ChildNodeFacetTreeBuilder();
		ftb.initialiseParameters(params);
	}

	@Test(expected=org.apache.solr.search.SyntaxError.class)
	public void initialiseParameters_missingNodeFieldParam() throws Exception {
		final SolrParams params = mock(SolrParams.class);
		when(params.get(HierarchicalFacets.NODE_FIELD_PARAM)).thenReturn(null);
		when(params.get(QueryParsing.V)).thenReturn(null);
		
		ChildNodeFacetTreeBuilder ftb = new ChildNodeFacetTreeBuilder();
		ftb.initialiseParameters(params);
		
		verify(params).get(HierarchicalFacets.NODE_FIELD_PARAM);
		verify(params).get(QueryParsing.V);
	}

	@Test(expected=org.apache.solr.search.SyntaxError.class)
	public void initialiseParameters_missingChildField() throws Exception {
		final String nodeField = "node";
		final String childField = null;
		final SolrParams params = mock(SolrParams.class);
		when(params.get(HierarchicalFacets.NODE_FIELD_PARAM)).thenReturn(null);
		when(params.get(QueryParsing.V)).thenReturn(nodeField);
		when(params.get(HierarchicalFacets.CHILD_FIELD_PARAM)).thenReturn(childField);
		
		ChildNodeFacetTreeBuilder ftb = new ChildNodeFacetTreeBuilder();
		ftb.initialiseParameters(params);
		
		verify(params).get(HierarchicalFacets.NODE_FIELD_PARAM);
		verify(params).get(QueryParsing.V);
		verify(params).get(HierarchicalFacets.CHILD_FIELD_PARAM);
	}

	@Test
	public void initialiseParameters() throws Exception {
		final String nodeField = "node";
		final String childField = "child";
		final SolrParams params = mock(SolrParams.class);
		when(params.get(HierarchicalFacets.NODE_FIELD_PARAM)).thenReturn(nodeField);
		when(params.get(HierarchicalFacets.CHILD_FIELD_PARAM)).thenReturn(childField);
		
		ChildNodeFacetTreeBuilder ftb = new ChildNodeFacetTreeBuilder();
		ftb.initialiseParameters(params);
		
		verify(params).get(HierarchicalFacets.NODE_FIELD_PARAM);
		verify(params).get(HierarchicalFacets.CHILD_FIELD_PARAM);
		verify(params).get(HierarchicalFacets.LABEL_FIELD_PARAM, null);
		verify(params).getInt(HierarchicalFacets.LEVELS_PARAM, 0);
	}

}
