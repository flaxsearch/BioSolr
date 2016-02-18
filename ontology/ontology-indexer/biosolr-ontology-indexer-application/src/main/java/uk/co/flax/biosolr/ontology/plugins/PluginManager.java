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
package uk.co.flax.biosolr.ontology.plugins;

import static uk.co.flax.biosolr.ontology.config.PluginConfiguration.ENTRY_PLUGIN_KEY;
import static uk.co.flax.biosolr.ontology.config.PluginConfiguration.ONTOLOGY_PLUGIN_KEY;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.api.OntologyEntryBean;
import uk.co.flax.biosolr.ontology.config.OntologyConfiguration;
import uk.co.flax.biosolr.ontology.config.PluginConfiguration;
import uk.co.flax.biosolr.ontology.loaders.OntologyLoader;

/**
 * Singleton class for managing all of the plugins used while indexing the ontology.
 * 
 * @author Matt Pearce
 */
public class PluginManager {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PluginManager.class);
	
	private final Map<String, Map<String, PluginConfiguration>> configuration;
	
	private final Map<String, Map<String, Plugin>> plugins = new HashMap<>();
	
	private static PluginManager pluginManager;

	private PluginManager(Map<String,  Map<String, PluginConfiguration>> configuration) throws PluginInitialisationException {
		this.configuration = configuration;
		initialisePlugins();
	}
	
	/**
	 * Initialise the plugin manager.
	 * @param configuration a map of plugin types, with each value holding plugin name
	 * to configuration details mappings.
	 * @throws PluginInitialisationException if the plugins cannot be initialised.
	 */
	public static void initialisePluginManager(Map<String,  Map<String, PluginConfiguration>> configuration) throws PluginInitialisationException{
		if (pluginManager != null) {
			throw new PluginInitialisationException("Plugin manager has already been initialised.");
		}
		
		pluginManager = new PluginManager(configuration);
	}
	
	/**
	 * Get the singleton {@link PluginManager} instance.
	 * @return the plugin manager.
	 */
	public static PluginManager getInstance() {
		return pluginManager;
	}
	
	private void initialisePlugins() throws PluginInitialisationException {
		try {
			initialisePlugins(ONTOLOGY_PLUGIN_KEY, configuration.get(ONTOLOGY_PLUGIN_KEY));
			initialisePlugins(ENTRY_PLUGIN_KEY, configuration.get(ENTRY_PLUGIN_KEY));
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			LOGGER.error("Cannot initialise plugins: {}", e.getMessage());
			throw new PluginInitialisationException(e);
		}
	}
	
	private void initialisePlugins(String pluginType, Map<String, PluginConfiguration> configMap) throws PluginInitialisationException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		if (configMap == null) {
			LOGGER.info("No plugins defined for type {}", pluginType);
		} else {
			plugins.put(pluginType, new HashMap<String, Plugin>());
			
			for (String pluginName : configMap.keySet()) {
				PluginConfiguration config = configMap.get(pluginName);

				Plugin plugin = (Plugin)Class.forName(config.getPluginClass()).newInstance();
				if (plugin == null) {
					throw new PluginInitialisationException("No " + pluginType + " '" + pluginName + "' plugin available");
				}

				plugin.initialise(config.getConfiguration());
				plugins.get(pluginType).put(pluginName, plugin);
			}
		}
	}
	
	public void processOntologyEntryPlugins(OntologyEntryBean entry, String sourceName, OntologyConfiguration ontologyConfiguration) throws PluginException {
		if (plugins.containsKey(ENTRY_PLUGIN_KEY)) {
			Map<String, Plugin> pluginMap = plugins.get(ENTRY_PLUGIN_KEY);
			for (String pluginName : pluginMap.keySet()) {
				OntologyEntryPlugin plugin = (OntologyEntryPlugin)pluginMap.get(pluginName);
				plugin.process(entry, sourceName, ontologyConfiguration);
			}
		}
	}
	
	public void processOntologyPlugins(OntologyLoader loader, String sourceName, OntologyConfiguration ontologyConfiguration) throws PluginException {
		if (plugins.containsKey(ONTOLOGY_PLUGIN_KEY)) {
			Map<String, Plugin> pluginMap = plugins.get(ONTOLOGY_PLUGIN_KEY);
			for (String pluginName : pluginMap.keySet()) {
				OntologyPlugin plugin = (OntologyPlugin)pluginMap.get(pluginName);
				plugin.process(loader, sourceName, ontologyConfiguration);
			}
		}
	}
	
	public void shutdownPlugins() {
		for (String pluginType : plugins.keySet()) {
			for (String pluginName : plugins.get(pluginType).keySet()) {
				try {
					Plugin plugin = plugins.get(pluginType).get(pluginName);
					plugin.shutdown();
				} catch (PluginException e) {
					// Log the error, carry on shutting down plugins
					LOGGER.error("Exception shutting down {} plugin {}: {}", pluginType, pluginName, e.getMessage());
				}
			}
		}
	}

}
