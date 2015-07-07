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

import uk.co.flax.biosolr.ontology.config.OntologyConfiguration;
import uk.co.flax.biosolr.ontology.indexer.loaders.OntologyLoader;

/**
 * Interface describing the functionality of a plugin which runs
 * across an entire ontology, such as a triple store builder.
 * 
 * @author Matt Pearce
 */
public interface OntologyPlugin extends Plugin {
	
	/**
	 * Process an ontology.
	 * @param loader the ontology loader.
	 * @param sourceName the name of the ontology to process.
	 * @param ontologyConfiguration the configuration details for the ontology.
	 * @throws PluginException if a problem occurs while processing the ontology.
	 */
	public void process(OntologyLoader loader, String sourceName, OntologyConfiguration ontologyConfiguration) throws PluginException;
	
}
