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
package uk.co.flax.biosolr.ontology.storage;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.config.StorageConfiguration;
import uk.co.flax.biosolr.ontology.storage.solr.SolrStorageEngine;

/**
 * Factory for constructing a {@link StorageEngine}, depending on the
 * details in the {@link StorageConfiguration} details.
 * 
 * @author Matt Pearce
 */
public class StorageEngineFactory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StorageEngineFactory.class);
	
	static final String SOLR_ENGINE_TYPE = "solr";
	
	public static StorageEngine buildStorageEngine(StorageConfiguration config) {
		StorageEngine ret = null;
		
		if (StringUtils.isBlank(config.getEngineType())) {
			LOGGER.error("No defined config engine type");
		} else {
			if (config.getEngineType().equals(SOLR_ENGINE_TYPE)) {
				ret = new SolrStorageEngine(config.getSolr());
			} else {
				LOGGER.error("Unrecognised storage engine type {}", config.getEngineType());
			}
		}
		
		return ret;
	}

}
