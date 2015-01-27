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
package uk.co.flax.biosolr.ontology.loaders;

import java.io.FileReader;
import java.io.IOException;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import uk.co.flax.biosolr.ontology.config.IndexerConfiguration;

/**
 * Configuration loader to read configuration details from a YAML file.
 * @author Matt Pearce
 */
public class YamlConfigurationLoader implements ConfigurationLoader {
	
	final String configFile;

	public YamlConfigurationLoader(String configFile) {
		this.configFile = configFile;
	}

	@Override
	public IndexerConfiguration fetchConfig() throws IOException {
		FileReader reader = new FileReader(configFile);
		Yaml yaml = new Yaml(new Constructor(IndexerConfiguration.class));
		
		// Load the config from the YAML file
		IndexerConfiguration config = (IndexerConfiguration)yaml.load(reader);
		
		// Close the file reader
		reader.close();
		
		return config;
	}

}
