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

package uk.co.flax.biosolr.ontology.documents.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.config.StorageConfiguration;
import uk.co.flax.biosolr.ontology.config.StorageEngineConfiguration;
import uk.co.flax.biosolr.ontology.documents.storage.solr.SolrStorageEngine;

/**
 * Factory class for creating and initialising a storage engine.
 *
 * @author mlp
 */
public class StorageEngineFactory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StorageEngineFactory.class);
	
	static final String SOLR_ENGINE_TYPE = "solr";
	
	private final StorageConfiguration config;
	
	public StorageEngineFactory(StorageConfiguration config) {
		this.config = config;
	}
	
	public StorageEngine buildStorageEngine(String engineType) throws StorageEngineException {
		StorageEngine engine;

		if (SOLR_ENGINE_TYPE.equals(engineType)) {
			// Solr is a special case, with own config class
			engine = new SolrStorageEngine(config.getSolr());
		} else {
			// Build the engine from the additional config
			engine = constructEngineFromConfiguration(config.getAdditionalEngines().get(engineType));
		}
		
		return engine;
	}
	
	private StorageEngine constructEngineFromConfiguration(StorageEngineConfiguration engineConfig) 
	throws StorageEngineException {
		StorageEngine ret = null;
		
		if (engineConfig != null) {
			try {
				ret = (StorageEngine) Class.forName(engineConfig.getEngineClass()).newInstance();
				ret.setConfiguration(engineConfig.getConfiguration());
				ret.initialise();
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				LOGGER.error("Exception thrown building storage engine {}: {}", engineConfig.getEngineClass(),
						e.getMessage());
				throw new StorageEngineException(e);
			}
		}
		
		return ret;
	}
	
}
