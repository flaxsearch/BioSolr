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

import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

import uk.co.flax.biosolr.ontology.api.OntologyEntryBean;
import uk.co.flax.biosolr.ontology.config.SolrConfiguration;
import uk.co.flax.biosolr.ontology.storage.StorageEngine;
import uk.co.flax.biosolr.ontology.storage.StorageEngineException;

/**
 * @author Matt Pearce
 */
public class SolrStorageEngine implements StorageEngine {
	
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

	/* (non-Javadoc)
	 * @see uk.co.flax.biosolr.ontology.storage.StorageEngine#isReady()
	 */
	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see uk.co.flax.biosolr.ontology.storage.StorageEngine#storeOntologyEntry(uk.co.flax.biosolr.ontology.api.OntologyEntryBean)
	 */
	@Override
	public void storeOntologyEntry(OntologyEntryBean entry) throws StorageEngineException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see uk.co.flax.biosolr.ontology.storage.StorageEngine#storeOntologyEntries(java.util.List)
	 */
	@Override
	public void storeOntologyEntries(List<OntologyEntryBean> entries) throws StorageEngineException {
		// TODO Auto-generated method stub

	}

}
