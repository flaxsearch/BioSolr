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
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.api.Document;
import uk.co.flax.biosolr.ontology.config.SolrConfiguration;
import uk.co.flax.biosolr.ontology.search.DocumentSearch;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

/**
 * @author Matt Pearce
 */
public class SolrDocumentSearch extends SolrSearchEngine implements DocumentSearch {

	private static final Logger LOGGER = LoggerFactory.getLogger(SolrDocumentSearch.class);

	private static final String EFO_URI_FIELD = "efo_uri";

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
	public ResultsList<Document> searchDocuments(String term, int start, int rows) throws SearchEngineException {
		ResultsList<Document> results = null;

		try {
			SolrQuery query = new SolrQuery(term);
			query.setStart(start);
			query.setRows(rows);
			query.setRequestHandler(config.getDocumentRequestHandler());
			LOGGER.debug("Query: {}", query);

			QueryResponse response = server.query(query);
			List<Document> docs;
			if (response.getResults().getNumFound() == 0) {
				docs = new ArrayList<>();
			} else {
				docs = response.getBeans(Document.class);
			}
			results = new ResultsList<>(docs, start, (start / rows), response.getResults().getNumFound());
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
