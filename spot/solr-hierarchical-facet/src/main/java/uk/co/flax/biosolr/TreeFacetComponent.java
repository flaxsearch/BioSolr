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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.FacetComponent;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreeFacetComponent extends FacetComponent {

	public static final String FACET_TREE = FacetParams.FACET + ".tree";
	public static final String FACET_TREE_FIELD = FACET_TREE + ".field";

	private static final Logger LOGGER = LoggerFactory.getLogger(TreeFacetComponent.class);
	
	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
		super.prepare(rb);
		
		// Do we need to create a facet tree?
		if (rb.doFacets && rb.req.getParams().getBool(FACET_TREE, false)) {
			try {
				LOGGER.debug("{} set to true - adding tree fields to facets", FACET_TREE);
				// Make sure the facet tree field is in the facet field list
				addTreeFieldsToFacets(rb);
			} catch (SyntaxError e) {
				throw new SolrException(ErrorCode.BAD_REQUEST, e);
			}
		}
	}
	
	private void addTreeFieldsToFacets(ResponseBuilder rb) throws SyntaxError {
		String[] ftFields = rb.req.getParams().getParams(FACET_TREE_FIELD);
		if (ftFields == null || ftFields.length == 0) {
			LOGGER.warn("No facet tree fields specified - ignoring facet trees");
		} else {
			// Take a modifiable copy of the incoming params
			ModifiableSolrParams params = new ModifiableSolrParams(rb.req.getParams());

			// Put the original facet fields (if any) into a Set
			Set<String> facetFields = new LinkedHashSet<>();
			if (params.getParams(FacetParams.FACET_FIELD) != null) {
				facetFields.addAll(Arrays.asList(params.getParams(FacetParams.FACET_FIELD)));
			}
			
			// Add the facet tree fields
			for (String ftField : ftFields) {
				// Parse the facet tree field, so we only add the field value,
				// rather than the whole string (ensure it's unique)
				SolrParams p = QueryParsing.getLocalParams(ftField, params);
				facetFields.add(p.get(QueryParsing.V));
			}
			
			// Add the (possibly) new facet fields
			params.set(FacetParams.FACET_FIELD, facetFields.toArray(new String[facetFields.size()]));

			// Re-set the params in the request
			rb.req.setParams(params);
		}
	}

	@Override
	public void process(ResponseBuilder rb) throws IOException {
		// Handle the initial facets
		super.process(rb);

		// And do the facet tree, if required
		if (rb.doFacets && rb.req.getParams().getBool(FACET_TREE, false)) {
			HierarchicalFacets ftp = new HierarchicalFacets(rb.req, rb.getResults().docSet, rb.req.getParams(), rb);
			@SuppressWarnings("rawtypes")
			SimpleOrderedMap<NamedList> ftpResponse = ftp.process(rb.req.getParams().getParams(FACET_TREE_FIELD));
			
			@SuppressWarnings("unchecked")
			NamedList<Object> facetCounts = (NamedList<Object>) rb.rsp.getValues().get("facet_counts");
			if (facetCounts != null) {
				facetCounts.add("facet_trees", ftpResponse);
			} else {
				facetCounts = new NamedList<>();
				facetCounts.add("facet_trees", ftpResponse);
				rb.rsp.add("facet_counts", facetCounts);
			}
		}
	}
	
}
