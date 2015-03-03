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
package uk.co.flax.biosolr.ontology.tdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.TDBLoader;
import com.hp.hpl.jena.tdb.sys.TDBInternal;

import uk.co.flax.biosolr.ontology.config.TripleStoreConfiguration;

/**
 * Class implementing building of a triple store alongside indexing the
 * ontologies.
 * 
 * @author Matt Pearce
 */
public class TripleStoreBuilder {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TripleStoreBuilder.class);
	
	private final DatasetGraph dataset;
	
	public TripleStoreBuilder(TripleStoreConfiguration config) {
		if (config.isBuildTripleStore()) {
			this.dataset = TDBFactory.createDatasetGraph(config.getTdbPath());
		} else {
			this.dataset = null;
		}
	}
	
	public void loadDataset(String ontologyUri) {
		LOGGER.debug("Loading dataset into triple store from {}", ontologyUri);
		TDBLoader.load(TDBInternal.getDatasetGraphTDB(dataset), ontologyUri, true);
	}
	
	public void closeDataset() {
		if (dataset != null) {
			dataset.close();
		}
	}

}
