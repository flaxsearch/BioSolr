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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Unit tests for the FacetTreeProcessor.
 */
public class FacetTreeProcessorTest {
	
	@Test
	public void process_noFacets() throws Exception {
		// Cannot mock local variable in ResponseBuilder...
		ResponseBuilder rb = new ResponseBuilder(null, null, null);
		rb.doFacets = false;
		
		SolrQueryRequest req = mock(SolrQueryRequest.class);
		
		HierarchicalFacets ftp = new HierarchicalFacets(req, null, null, rb);
		assertNull(ftp.process(new String[]{ "blah" }));
	}

	@Test
	public void process_noFacetTreeParams() throws Exception {
		// Cannot mock local variable in ResponseBuilder...
		ResponseBuilder rb = new ResponseBuilder(null, null, null);
		rb.doFacets = true;
		
		SolrQueryRequest req = mock(SolrQueryRequest.class);
		
		HierarchicalFacets ftp = new HierarchicalFacets(req, null, null, rb);
		assertNull(ftp.process(null));
	}

	@Test(expected=org.apache.solr.common.SolrException.class)
	public void process_noChildParamGiven() throws Exception {
		// Cannot mock local variable in ResponseBuilder...
		ResponseBuilder rb = new ResponseBuilder(null, null, null);
		rb.doFacets = true;
		
		SolrQueryRequest req = mock(SolrQueryRequest.class);
		SolrParams params = mock(SolrParams.class);
		when(req.getParams()).thenReturn(params);

		final String[] facetTrees = new String[] { "{!" + HierarchicalFacets.LOCAL_PARAM_TYPE + " x=y}uri" };
		HierarchicalFacets ftp = new HierarchicalFacets(req, null, null, rb);
		ftp.process(facetTrees);
	}

	@Test @Ignore
	public void process_childParamGiven() throws Exception {
		// Cannot mock local variable in ResponseBuilder...
		ResponseBuilder rb = new ResponseBuilder(null, null, null);
		rb.doFacets = true;
		
		SolrQueryRequest req = mock(SolrQueryRequest.class);

		final String[] facetTrees = new String[] { "{!" + HierarchicalFacets.LOCAL_PARAM_TYPE + HierarchicalFacets.CHILD_FIELD_PARAM + "=child_uris}uri" };
		HierarchicalFacets ftp = new HierarchicalFacets(req, null, null, rb);
		assertNotNull(ftp.process(facetTrees));
	}

}