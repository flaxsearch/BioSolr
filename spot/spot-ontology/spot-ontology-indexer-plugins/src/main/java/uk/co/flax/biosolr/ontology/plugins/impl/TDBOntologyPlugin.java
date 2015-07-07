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
package uk.co.flax.biosolr.ontology.plugins.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.config.OntologyConfiguration;
import uk.co.flax.biosolr.ontology.indexer.loaders.OntologyLoader;
import uk.co.flax.biosolr.ontology.plugins.OntologyPlugin;
import uk.co.flax.biosolr.ontology.plugins.Plugin;
import uk.co.flax.biosolr.ontology.plugins.PluginException;
import uk.co.flax.biosolr.ontology.plugins.PluginInitialisationException;

import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.TDBLoader;
import com.hp.hpl.jena.tdb.sys.TDBInternal;

/**
 * @author Matt Pearce
 */
public class TDBOntologyPlugin implements OntologyPlugin {
	
	public static final String PLUGIN_NAME = "tripleStore";
	
	static final String ENABLED_CFGKEY = "enabled";
	static final String TDB_PATH_CFGKEY = "tdbPath";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TDBOntologyPlugin.class);

    private boolean enabled;
	private DatasetGraph dataset;
	
	public TDBOntologyPlugin() {
		// Do nothing - no configuration available until initialise() called
	}

	@Override
	public Plugin createPlugin() {
		return new TDBOntologyPlugin();
	}

	@Override
	public void initialise(Map<String, Object> configuration) throws PluginInitialisationException {
		LOGGER.debug("Initialising ontology plugin: {}", PLUGIN_NAME);
		if (!configuration.containsKey(ENABLED_CFGKEY)) {
			LOGGER.info("No '{}' config key - assuming plugin disabled", ENABLED_CFGKEY);
		} else {
            enabled = (Boolean)configuration.get(ENABLED_CFGKEY);
            if (enabled) {
                if (!configuration.containsKey(TDB_PATH_CFGKEY)) {
                    throw new PluginInitialisationException("No " + TDB_PATH_CFGKEY + " specified - cannot create TDB dataset.");
                } else {
                    this.dataset = TDBFactory.createDatasetGraph((String) configuration.get(TDB_PATH_CFGKEY));
                }
            }
		}
	}

	@Override
	public void shutdown() throws PluginException {
		LOGGER.debug("Shutting down ontology plugin: {}", PLUGIN_NAME);
		if (dataset != null) {
			dataset.close();
		}
	}

	@Override
	public void process(OntologyLoader loader, String sourceName, OntologyConfiguration ontologyConfiguration) throws PluginException {
        if (enabled) {
            LOGGER.debug("Loading dataset {} into triple store from {}", sourceName, ontologyConfiguration.getAccessURI());
            TDBLoader.load(TDBInternal.getDatasetGraphTDB(dataset), ontologyConfiguration.getAccessURI(), true);
        } else {
            LOGGER.debug("TDB plugin disabled - skipping");
        }
	}

}
