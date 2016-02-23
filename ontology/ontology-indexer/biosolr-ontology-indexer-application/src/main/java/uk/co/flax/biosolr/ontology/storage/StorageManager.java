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

package uk.co.flax.biosolr.ontology.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.api.OntologyEntryBean;
import uk.co.flax.biosolr.ontology.config.StorageConfiguration;

/**
 * StorageManager is a wrapper class around all the storage engines that
 * may be in use.
 *
 * @author mlp
 */
public class StorageManager implements StorageEngine {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StorageManager.class);
	
	private final StorageConfiguration config;
	private final StorageEngineFactory engineFactory;
	
	private List<StorageEngine> engines;
	
	public StorageManager(StorageConfiguration config) {
		this(config, new StorageEngineFactory(config));
	}
	
	// Unit testing constructor
	StorageManager(StorageConfiguration config, StorageEngineFactory factory) {
		this.config = config;
		this.engineFactory = factory;
	}

	@Override
	public void setConfiguration(Map<String, Object> configuration) throws StorageEngineException {
	}

	@Override
	public void initialise() throws StorageEngineException {
		engines = new ArrayList<>(config.getEngineTypes().size());
		
		for (String type : config.getEngineTypes()) {
			StorageEngine engine = engineFactory.buildStorageEngine(type);
			if (engine == null) {
				throw new StorageEngineException("No StorageEngine could be found for " + type);
			}
			engines.add(engine);
		}
	}

	@Override
	public boolean isReady() {
		boolean ret = true;
		
		for (StorageEngine engine : engines) {
			if (!engine.isReady()) {
				ret = false;
				LOGGER.warn("Engine {} is not ready", engine.getClass().getName());
			}
		}
		
		return ret;
	}

	@Override
	public void storeOntologyEntry(OntologyEntryBean entry) throws StorageEngineException {
		// TODO: May be better to spawn threads for this
		for (StorageEngine engine : engines) {
			engine.storeOntologyEntry(entry);
		}
	}

	@Override
	public void storeOntologyEntries(List<OntologyEntryBean> entries) throws StorageEngineException {
		// TODO: May be better to spawn threads for this
		for (StorageEngine engine : engines) {
			engine.storeOntologyEntries(entries);
		}
	}

}
