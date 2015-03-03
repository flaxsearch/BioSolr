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

import java.util.Map;

/**
 * Configuration details for Ontology indexer application.
 * 
 * @author Matt Pearce
 */
public class IndexerConfiguration {
	
	private String ontologySolrUrl;
	private String documentsSolrUrl;
	
	private Map<String, OntologyConfiguration> ontologies;
	
	private DatabaseConfiguration database;
	
	private StorageConfiguration storage;
	
	private Map<String, Map<String, PluginConfiguration>> pluginTypes;

	/**
	 * @return the ontologySolrUrl
	 */
	public String getOntologySolrUrl() {
		return ontologySolrUrl;
	}

	/**
	 * @return the documentSolrUrl
	 */
	public String getDocumentsSolrUrl() {
		return documentsSolrUrl;
	}

	/**
	 * @return the database
	 */
	public DatabaseConfiguration getDatabase() {
		return database;
	}

	/**
	 * @param ontologySolrUrl the ontologySolrUrl to set
	 */
	public void setOntologySolrUrl(String ontologySolrUrl) {
		this.ontologySolrUrl = ontologySolrUrl;
	}

	/**
	 * @param documentSolrUrl the documentSolrUrl to set
	 */
	public void setDocumentsSolrUrl(String documentSolrUrl) {
		this.documentsSolrUrl = documentSolrUrl;
	}

	/**
	 * @param database the database to set
	 */
	public void setDatabase(DatabaseConfiguration database) {
		this.database = database;
	}

	/**
	 * @return the ontologies
	 */
	public Map<String, OntologyConfiguration> getOntologies() {
		return ontologies;
	}

	/**
	 * @param ontologies the ontologies to set
	 */
	public void setOntologies(Map<String, OntologyConfiguration> ontologies) {
		this.ontologies = ontologies;
	}

	/**
	 * @return the storage
	 */
	public StorageConfiguration getStorage() {
		return storage;
	}

	/**
	 * @param storage the storage to set
	 */
	public void setStorage(StorageConfiguration storage) {
		this.storage = storage;
	}

	/**
	 * @return the plugin
	 */
	public Map<String,  Map<String, PluginConfiguration>> getPluginTypes() {
		return pluginTypes;
	}

	/**
	 * @param plugin the plugin to set
	 */
	public void setPluginTypes(Map<String,  Map<String, PluginConfiguration>> plugin) {
		this.pluginTypes = plugin;
	}

}
