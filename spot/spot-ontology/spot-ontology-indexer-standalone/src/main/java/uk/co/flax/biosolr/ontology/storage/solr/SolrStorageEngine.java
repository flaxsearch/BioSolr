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
package uk.co.flax.biosolr.ontology.storage.solr;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.api.OntologyEntryBean;
import uk.co.flax.biosolr.ontology.config.SolrConfiguration;
import uk.co.flax.biosolr.ontology.storage.StorageEngine;
import uk.co.flax.biosolr.ontology.storage.StorageEngineException;

/**
 * @author Matt Pearce
 */
public class SolrStorageEngine implements StorageEngine {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SolrStorageEngine.class);
	
	static final int STATUS_OK = 0;
	
	private final SolrConfiguration config;
	private final SolrServer server;
	
	public SolrStorageEngine(SolrConfiguration config) {
		this(config, new HttpSolrServer(config.getBaseUrl()));
	}
	
	// Unit testing constructor with incoming SolrServer
	SolrStorageEngine(SolrConfiguration config, SolrServer server) {
		this.config = config;
		this.server = server;
	}

	@Override
	public boolean isReady() {
		boolean ready = false;

		try {
			SolrPingResponse response = server.ping();
			ready = (response != null && response.getStatus() == STATUS_OK);

			if (!ready) {
				if (response == null) {
					LOGGER.error("Search engine returned null response from ping()");
				} else {
					LOGGER.error("Search engine is not ready: ", response.getResponse());
				}
			}
		} catch (SolrServerException e) {
			LOGGER.error("Server exception from ping(): {}", e.getMessage());
		} catch (IOException e) {
			LOGGER.error("IO exception when calling server: {}", e.getMessage());
		}

		return ready;
	}

	@Override
	public void storeOntologyEntry(OntologyEntryBean entry) throws StorageEngineException {
		storeOntologyEntries(Arrays.asList(entry));
	}

	@Override
	public void storeOntologyEntries(List<OntologyEntryBean> entries) throws StorageEngineException {
		if (entries.isEmpty()) {
			LOGGER.debug("storeOntologyEntries called with empty list - ignoring");
		} else {
			try {
				UpdateResponse response = server.addBeans(entries, config.getCommitWithinMs());
				if (response.getStatus() != STATUS_OK) {
					LOGGER.error("Unexpected response: {}", response.getResponse());
					throw new StorageEngineException("Could not store entries: " + response.getResponse());
				}
			} catch (IOException e) {
				LOGGER.error("IO exception when calling server: {}", e.getMessage());
				throw new StorageEngineException(e);
			} catch (SolrServerException e) {
				LOGGER.error("Server exception when storing entries: {}", e.getMessage());
				throw new StorageEngineException(e);
			}
		}
	}

}
