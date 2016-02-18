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

import java.io.FileReader;
import java.io.IOException;

import uk.co.flax.biosolr.ontology.config.IndexerConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Configuration loader to read configuration details from a YAML file.
 * @author Matt Pearce
 */
public class YamlConfigurationLoader implements ConfigurationLoader {
	
	private final String configFile;
	private final YAMLFactory yamlFactory;
	private final ObjectMapper mapper;

	public YamlConfigurationLoader(String configFile) {
		this.configFile = configFile;
		this.yamlFactory = new YAMLFactory();
		this.mapper = new ObjectMapper();
	}

	@Override
	public IndexerConfiguration loadConfiguration() throws IOException {
		FileReader reader = new FileReader(configFile);
        final JsonNode node = mapper.readTree(yamlFactory.createParser(reader));
        final IndexerConfiguration config = mapper.readValue(new TreeTraversingParser(node), IndexerConfiguration.class);
		
		// Close the file reader
		reader.close();
		
		return config;
	}

}
