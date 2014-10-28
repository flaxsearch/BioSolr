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

	private final SolrConfiguration config;
	private final SolrServer server;
	
	public SolrDocumentSearch(SolrConfiguration config) {
		this.config = config;
		this.server = new HttpSolrServer(config.getDocumentUrl());
	}
	
	protected SolrServer getServer() {
		return server;
	}
	
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

			QueryResponse response = server.query(query);
			List<Document> docs = response.getBeans(Document.class);
			results = new ResultsList<>(docs, start, (start / rows), response.getResults().getNumFound());
		} catch (SolrServerException e) {
			throw new SearchEngineException(e);
		}
		
		return results;
	}

	@Override
	public ResultsList<Document> searchByEfoUri(int start, int rows, String... uris) throws SearchEngineException {
		// TODO Auto-generated method stub
		return null;
	}

}
