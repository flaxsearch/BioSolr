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

import java.util.Map;

/**
 * Generic plugin interface, defining initialise and shutdown methods.
 * Each plugin implementation should have a zero-argument constructor.
 * 
 * @author Matt Pearce
 */
public interface Plugin {
	
	/**
	 * Create an instance of this plugin.
	 * @return the plugin instance.
	 */
	public Plugin createPlugin();

	/**
	 * Initialise the plugin, using configuration supplied as a map of
	 * objects.
	 * @param configuration the configuration details.
	 */
	public void initialise(Map<String, Object> configuration) throws PluginException;
	
	/**
	 * Shut down the plugin. This allows for any resources being used to be closed
	 * cleanly.
	 * @throws PluginException
	 */
	public void shutdown() throws PluginException;

}
