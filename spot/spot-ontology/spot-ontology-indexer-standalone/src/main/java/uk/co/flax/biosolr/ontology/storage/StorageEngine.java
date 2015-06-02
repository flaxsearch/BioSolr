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

import java.util.List;

import uk.co.flax.biosolr.ontology.api.OntologyEntryBean;

/**
 * Interface defining functionality to store ontology entries in a
 * search engine or other data store.
 * 
 * @author Matt Pearce
 */
public interface StorageEngine {
	
	/**
	 * Check whether the storage engine is available.
	 * @return <code>true</code> if the engine is available, <code>false</code> if not.
	 */
	public boolean isReady();
	
	/**
	 * Store a single ontology entry.
	 * @param entry the entry to store.
	 * @throws StorageEngineException if the entry cannot be stored.
	 */
	public void storeOntologyEntry(OntologyEntryBean entry) throws StorageEngineException;
	
	/**
	 * Store a batch of ontology entries.
	 * @param entries the entries to be stored.
	 * @throws StorageEngineException if the entries cannot be stored.
	 */
	public void storeOntologyEntries(List<OntologyEntryBean> entries) throws StorageEngineException;

}
