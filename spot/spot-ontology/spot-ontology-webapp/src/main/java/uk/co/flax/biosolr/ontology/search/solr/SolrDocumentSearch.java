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
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.GroupResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.DisMaxParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.api.Document;
import uk.co.flax.biosolr.ontology.config.SolrConfiguration;
import uk.co.flax.biosolr.ontology.search.DocumentSearch;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

/**
 * Solr-specific implementation of the {@link DocumentSearch} search engine.
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
	public ResultsList<Document> searchDocuments(String term, int start, int rows, List<String> additionalFields) throws SearchEngineException {
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
			query.setParam(DisMaxParams.QF, queryFields.toArray(new String[queryFields.size()]));
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
			results = new ResultsList<>(docs, start, (start / rows), total);
		} catch (SolrServerException e) {
			throw new SearchEngineException(e);
		}

		return results;
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
