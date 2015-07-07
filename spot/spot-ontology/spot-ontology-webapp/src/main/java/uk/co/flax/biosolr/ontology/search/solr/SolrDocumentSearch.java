/**
 * Copyright (c) 2014 Lemur Consulting Ltd.
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
package uk.co.flax.biosolr.ontology.search.solr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.GroupResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.api.Document;
import uk.co.flax.biosolr.ontology.api.FacetEntry;
import uk.co.flax.biosolr.ontology.api.FacetStyle;
import uk.co.flax.biosolr.ontology.api.HierarchicalFacetEntry;
import uk.co.flax.biosolr.ontology.config.FacetTreeConfiguration;
import uk.co.flax.biosolr.ontology.config.SolrConfiguration;
import uk.co.flax.biosolr.ontology.search.DocumentSearch;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

/**
 * Solr-specific implementation of the {@link DocumentSearch} search engine.
 * 
 * @author Matt Pearce
 */
public class SolrDocumentSearch extends SolrSearchEngine implements DocumentSearch {

	private static final Logger LOGGER = LoggerFactory.getLogger(SolrDocumentSearch.class);

	private static final String EFO_URI_FIELD = "efo_uri";
	private static final String TITLE_FIELD = "title";
	private static final String FIRST_AUTHOR_FIELD = "first_author";
	private static final String PUBLICATION_FIELD = "publication";
	private static final String EFO_LABELS_FIELD = "efo_labels";
	
	private static final List<String> DEFAULT_SEARCH_FIELDS = new ArrayList<>();
	static {
		DEFAULT_SEARCH_FIELDS.add(TITLE_FIELD);
		DEFAULT_SEARCH_FIELDS.add(FIRST_AUTHOR_FIELD);
		DEFAULT_SEARCH_FIELDS.add(PUBLICATION_FIELD);
		DEFAULT_SEARCH_FIELDS.add(EFO_LABELS_FIELD);
	}

	private final SolrConfiguration config;
	private final SolrServer server;

	public SolrDocumentSearch(SolrConfiguration config) {
		this.config = config;
		this.server = new HttpSolrServer(config.getDocumentUrl());
	}

	@Override
	protected SolrServer getServer() {
		return server;
	}

	@Override
	protected Logger getLogger() {
		return LOGGER;
	}

	@Override
	public ResultsList<Document> searchDocuments(String term, int start, int rows, List<String> additionalFields,
			List<String> filters, FacetStyle facetStyle) throws SearchEngineException {
		ResultsList<Document> results = null;

		try {
			SolrQuery query = new SolrQuery(term);
			query.setStart(start);
			query.setRows(rows);
			query.setRequestHandler(config.getDocumentRequestHandler());
			List<String> queryFields = new ArrayList<>(DEFAULT_SEARCH_FIELDS);
			if (additionalFields != null) {
				queryFields.addAll(additionalFields);
			}
			if (filters != null) {
				query.addFilterQuery(filters.toArray(new String[0]));
			}
			query.setParam(DisMaxParams.QF, queryFields.toArray(new String[queryFields.size()]));
			
			if (facetStyle == FacetStyle.NONE) {
				query.addFacetField(config.getFacetFields().toArray(new String[config.getFacetFields().size()]));
			} else {
				// Add the facet tree params
				query.setFacet(true);
				query.setParam("facet.tree", true);
				query.setParam("facet.tree.field", buildFacetTreeQueryParameter(facetStyle));
			}
			
			LOGGER.debug("Query: {}", query);

			QueryResponse response = server.query(query);
			List<Document> docs;
			long total = 0;
			
			if (response.getGroupResponse() != null) {
				docs = new ArrayList<>(rows);
				GroupResponse gResponse = response.getGroupResponse();
				for (GroupCommand gCommand : gResponse.getValues()) {
					total += gCommand.getNGroups();
					for (Group group : gCommand.getValues()) {
						docs.addAll(server.getBinder().getBeans(Document.class, group.getResult()));
					}
				}
			} else if (response.getResults().getNumFound() == 0) {
				docs = new ArrayList<>();
			} else {
				docs = response.getBeans(Document.class);
				total = response.getResults().getNumFound();
			}
			
			results = new ResultsList<>(docs, start, (start / rows), total, extractFacets(response, facetStyle));
		} catch (SolrServerException e) {
			throw new SearchEngineException(e);
		}

		return results;
	}
	
	private Map<String, List<FacetEntry>> extractFacets(QueryResponse response, FacetStyle facetStyle) {
		Map<String, List<FacetEntry>> facets = new HashMap<>();
		
		for (String name : config.getFacetFields()) {
			FacetField fld = response.getFacetField(name);
			if (fld != null && !fld.getValues().isEmpty()) {
				List<FacetEntry> facetValues = new ArrayList<>();
				for (Count count : fld.getValues()) {
					facetValues.add(new FacetEntry(count.getName(), count.getCount()));
				}
				facets.put(name, facetValues);
			}
		}
		
		// And extract the facet tree, if there is one
		if (facetStyle != FacetStyle.NONE) {
			List<Object> facetTree = findFacetTree(response, EFO_URI_FIELD);
			if (facetTree != null && !facetTree.isEmpty()) {
				facets.put(EFO_URI_FIELD + "_hierarchy", extractFacetTreeFromNamedList(facetTree));
			}
		}
		
		return facets;
	}
	
	@SuppressWarnings("unchecked")
	private List<Object> findFacetTree(QueryResponse response, String field) {
		NamedList<Object> baseResponse = response.getResponse();
		NamedList<Object> facetTrees = (NamedList<Object>) baseResponse.findRecursive("facet_counts", "facet_trees");
		
		return facetTrees == null ? null : facetTrees.getAll(field);
	}
	
	@SuppressWarnings("unchecked")
	private List<FacetEntry> extractFacetTreeFromNamedList(List<Object> facetTree) {
		List<FacetEntry> entries;
		if (facetTree == null) {
			entries = null;
		} else {
			entries = new ArrayList<>(facetTree.size());

			for (Object ftList : facetTree) {
				for (Object ft : (List<Object>)ftList) {
					NamedList<Object> nl = (NamedList<Object>)ft;

					String label = (String) nl.get("label");
					String value = (String) nl.get("value");
					long count = (long) nl.get("count");
					long total = (long) nl.get("total");
					List<FacetEntry> hierarchy = extractFacetTreeFromNamedList(nl.getAll("hierarchy"));

					entries.add(new HierarchicalFacetEntry(value, label, count, total, hierarchy));
				}
			}
		}
		
		return entries;
	}
	
	private String buildFacetTreeQueryParameter(FacetStyle style) {
		FacetTreeConfiguration ftConfig = config.getDocumentFacetTree();
		
		StringBuilder ftqParam = new StringBuilder("{!ftree");
		
		ftqParam.append(" childField=").append(ftConfig.getChildField());
		ftqParam.append(" nodeField=").append(ftConfig.getNodeField());
		ftqParam.append(" collection=").append(ftConfig.getCollection());
		if (StringUtils.isNotBlank(ftConfig.getLabelField())) {
			ftqParam.append(" labelField=").append(ftConfig.getLabelField());
		}
		
		// Handle the pruning parameters, if required
		if (style == FacetStyle.SIMPLE_PRUNE) {
			ftqParam.append(" prune=simple");
		} else if (style == FacetStyle.DATAPOINT_PRUNE) {
			ftqParam.append(" prune=datapoint datapoints=").append(ftConfig.getDatapoints());
		}
		
		ftqParam.append("}").append(ftConfig.getBaseField());
		
		return ftqParam.toString();
	}
	
	@Override
	public ResultsList<Document> searchByEfoUri(int start, int rows, String term, String... uris) throws SearchEngineException {
		ResultsList<Document> results = null;

		try {
			SolrQuery query = new SolrQuery(term + " OR " + EFO_URI_FIELD + ":" + buildUriFilter(uris));
			// query.addFilterQuery(EFO_URI_FIELD + ":" + buildUriFilter(uris));
			query.setStart(start);
			query.setRows(rows);
			query.setRequestHandler(config.getDocumentUriRequestHandler());

			LOGGER.debug("Solr query: {}", query);

			QueryResponse response = server.query(query);
			List<Document> docs = response.getBeans(Document.class);
			results = new ResultsList<>(docs, start, (start / rows), response.getResults().getNumFound());
		} catch (SolrServerException e) {
			throw new SearchEngineException(e);
		}

		return results;
	}

	private String buildUriFilter(String... uris) {
		StringBuilder builder = new StringBuilder();
		int count = 0;

		for (String uri : uris) {
			if (count > 0) {
				builder.append(" OR ");
			}
			builder.append('"').append(uri).append('"');
			count ++;
		}

		return builder.toString();
	}
	
}
