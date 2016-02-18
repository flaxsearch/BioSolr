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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class representing the configuration for a single plugin.
 * 
 * @author Matt Pearce
 */
public class PluginConfiguration {
	
	public static final String ONTOLOGY_PLUGIN_KEY = "ontology";
	public static final String ENTRY_PLUGIN_KEY = "entry";

	@JsonProperty("class")
	private String pluginClass;
	
	private Map<String, Object> configuration = new HashMap<>();

	/**
	 * @return the pluginClass
	 */
	public String getPluginClass() {
		return pluginClass;
	}

	/**
	 * @param pluginClass the pluginClass to set
	 */
	public void setPluginClass(String pluginClass) {
		this.pluginClass = pluginClass;
	}

	/**
	 * @return the configuration
	 */
	public Map<String, Object> getConfiguration() {
		return configuration;
	}

	/**
	 * @param configuration the configuration to set
	 */
	public void setConfiguration(Map<String, Object> configuration) {
		this.configuration = configuration;
	}

}
