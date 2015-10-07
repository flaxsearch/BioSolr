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
package uk.co.flax.biosolr.ontology.config;

/**
 * Configuration details for Ontology indexer application.
 * 
 * @author Matt Pearce
 */
public class IndexerConfiguration {

	private DatabaseConfiguration database;
	private StorageConfiguration storage;

	/**
	 * @return the database
	 */
	public DatabaseConfiguration getDatabase() {
		return database;
	}

	/**
	 * @param database the database to set
	 */
	public void setDatabase(DatabaseConfiguration database) {
		this.database = database;
	}

	public StorageConfiguration getStorage() {
		return storage;
	}

	public void setStorageEngine(StorageConfiguration storageEngine) {
		this.storage = storageEngine;
	}

}
