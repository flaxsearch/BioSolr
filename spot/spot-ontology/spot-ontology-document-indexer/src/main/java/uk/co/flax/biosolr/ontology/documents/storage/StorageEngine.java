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

import java.util.List;
import java.util.Map;

import uk.co.flax.biosolr.ontology.api.Document;
import uk.co.flax.biosolr.ontology.config.StorageEngineConfiguration;

/**
 * An engine for storing documents in a search engine.
 *
 * @author mlp
 */
public interface StorageEngine {
	
	/**
	 * Set the configuration details.
	 * @param configuration a map containing the configuration options for
	 * the implementation.
	 * @throws StorageEngineConfiguration if a required option is missing.
	 */
	void setConfiguration(Map<String, Object> configuration) throws StorageEngineException;
	
	/**
	 * Carry out any initialisation tasks required to make the storage engine
	 * ready to receive and store data.
	 * @throws StorageEngineException if any part of the initialisation 
	 * process fails.
	 */
	void initialise() throws StorageEngineException;
	
	/**
	 * Check whether the storage engine is available.
	 * @return <code>true</code> if the engine is available, <code>false</code> if not.
	 */
	boolean isReady();
	
	/**
	 * Store documents in the search engine.
	 * @param documents
	 * @throws StorageEngineException if problems occur while storing
	 * the documents.
	 */
	void storeDocuments(List<Document> documents) throws StorageEngineException;
	
	/**
	 * Flush any records not already committed.
	 * @throws StorageEngineException
	 */
	void flush() throws StorageEngineException;
	
}
