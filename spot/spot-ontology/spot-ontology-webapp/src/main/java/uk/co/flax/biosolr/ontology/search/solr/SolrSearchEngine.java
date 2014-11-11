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

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.slf4j.Logger;

import uk.co.flax.biosolr.ontology.search.SearchEngine;

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
	
	protected abstract SolrServer getServer();
	protected abstract Logger getLogger();

}
