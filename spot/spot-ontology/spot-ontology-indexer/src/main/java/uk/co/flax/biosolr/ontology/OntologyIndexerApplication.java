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
package uk.co.flax.biosolr.ontology;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.config.IndexerConfiguration;
import uk.co.flax.biosolr.ontology.indexer.OWLOntologyIndexer;
import uk.co.flax.biosolr.ontology.indexer.OntologyIndexer;
import uk.co.flax.biosolr.ontology.indexer.OntologyIndexingException;
import uk.co.flax.biosolr.ontology.loaders.ConfigurationLoader;
import uk.co.flax.biosolr.ontology.loaders.ConfigurationLoaderFactory;
import uk.co.flax.biosolr.ontology.storage.StorageEngine;
import uk.co.flax.biosolr.ontology.storage.StorageEngineFactory;

/**
 * Main class for indexing one or more ontologies.
 * 
 * @author Matt Pearce
 */
public class OntologyIndexerApplication {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OntologyIndexerApplication.class);
	
	private final IndexerConfiguration configuration;
	
	public OntologyIndexerApplication(IndexerConfiguration config) {
		this.configuration = config;
	}
	
	public void run() {
		StorageEngine storageEngine = StorageEngineFactory.buildStorageEngine(configuration.getStorage());
		if (storageEngine == null) {
			System.err.println("No storage engine - aborting!");
			return;
		} else if (!storageEngine.isReady()) {
			System.err.println("Storage engine is not ready - aborting!");
			return;
		}
		
		for (String source : configuration.getOntologies().keySet()) {
			try {
				OntologyIndexer indexer = new OWLOntologyIndexer(source, configuration.getOntologies().get(source),
						storageEngine);
				indexer.indexOntology();
			} catch (OntologyIndexingException e) {
				LOGGER.error("Caught exception indexing {}: {}", source, e.getMessage());
				LOGGER.error("Exception detail:", e);
			}
		}
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			usage();
			System.exit(1);
		}
		
        // Set property to make sure we can parse all of EFO
        System.setProperty("entityExpansionLimit", "1000000");
        
		ConfigurationLoader configLoader = ConfigurationLoaderFactory.buildConfigurationLoader(args[0]);
		if (configLoader == null) {
			System.err.println("Could not find configuration loader for " + args[0]);
			System.exit(1);
		}
		
		try {
			OntologyIndexerApplication indexer = new OntologyIndexerApplication(configLoader.loadConfiguration());
			indexer.run();
		} catch (IOException e) {
			System.err.println("Could not load configuration file " + args[0] + ": " + e.getMessage());
			System.exit(1);
		}
	}
	
	private static void usage() {
		System.out.println("Usage:");
		System.out.println("  java uk.co.flax.biosolr.ontology.OntologyIndexer configfile");
	}

}
