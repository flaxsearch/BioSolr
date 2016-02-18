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
package uk.co.flax.biosolr.ontology.config.loaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for building a configuration loader.
 * 
 * @author Matt Pearce
 */
public class ConfigurationLoaderFactory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationLoaderFactory.class);
	
	public static ConfigurationLoader buildConfigurationLoader(String configFile) {
		ConfigurationLoader ret = null;
		
		String ext = configFile.substring(configFile.lastIndexOf('.'));
		if (ext.equals(".yml")) {
			ret = new YamlConfigurationLoader(configFile);
		}
		
		if (ret == null) {
			LOGGER.error("No configuration loader found for file {}", configFile);
		}
		
		return ret;
	}

}
