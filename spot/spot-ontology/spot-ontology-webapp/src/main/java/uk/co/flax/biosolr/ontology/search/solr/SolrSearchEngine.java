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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;

import uk.co.flax.biosolr.ontology.search.SearchEngine;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

/**
 * @author Matt Pearce
 */
public abstract class SolrSearchEngine implements SearchEngine {

	@Override
	public boolean isSearchEngineReady() {
		boolean ready = false;

		try {
			SolrPingResponse response = getServer().ping();
			ready = (response != null && response.getStatus() == 0);

			if (!ready) {
				if (response == null) {
					getLogger().error("Search engine returned null response from ping()");
				} else {
					getLogger().error("Search engine is not ready: ", response.getResponse());
				}
			}
		} catch (SolrServerException e) {
			getLogger().error("Server exception from ping(): {}", e.getMessage());
		} catch (IOException e) {
			getLogger().error("IO exception when calling server: {}", e.getMessage());
		}

		return ready;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<String> getDynamicFieldNames() throws SearchEngineException {
		List<String> fields = new ArrayList<>();
		
		LukeRequest request = new LukeRequest();
		request.setNumTerms(0);
		request.setShowSchema(false);
		try {
			LukeResponse response = request.process(getServer());
			NamedList<Object> flds = (NamedList<Object>) response.getResponse().get("fields");
			if (flds != null) {
				for (Map.Entry<String, Object> field : flds) {
					String name = field.getKey();
					for (Entry<String, Object> prop : (NamedList<Object>)field.getValue()) {
						if ("dynamicBase".equals(prop.getKey())) {
							fields.add(name);
							break;
						}
					}
				}
			}

		} catch (SolrServerException e) {
			throw new SearchEngineException(e);
		} catch (IOException e) {
			throw new SearchEngineException(e);
		}
		
		return fields;
	}
	
	protected String getQueryUrl(SolrQuery query, String baseUrl) {
		StringBuilder queryUrl = new StringBuilder(baseUrl);
		if (StringUtils.isBlank(query.getRequestHandler())) {
			queryUrl.append("/select");
		} else {
			queryUrl.append(query.getRequestHandler());
		}
		queryUrl.append(ClientUtils.toQueryString(query, false));

		return queryUrl.toString();
	}
	
	protected abstract SolrServer getServer();
	protected abstract Logger getLogger();

}
