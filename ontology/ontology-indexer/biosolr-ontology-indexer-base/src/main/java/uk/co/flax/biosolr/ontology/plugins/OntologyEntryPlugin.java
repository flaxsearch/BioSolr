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

import uk.co.flax.biosolr.ontology.api.OntologyEntryBean;
import uk.co.flax.biosolr.ontology.config.OntologyConfiguration;

/**
 * Plugin interface defining functionality for a plugin to run across
 * a single ontology entry. This may be, for example, looking up some
 * additional details in a database to populate one or more fields.
 * 
 * @author Matt Pearce
 */
public interface OntologyEntryPlugin {
	
	/**
	 * Process an ontology entry. This method may modify the entry in-place,
	 * by adding or modifying field information.
	 * @param entry the entry to be processed.
	 * @param sourceName the name of the ontology from which the entry originated.
	 * @param ontologyConfiguration the configuration for the ontology.
	 * @throws PluginException if problems occur while processing the entry.
	 */
	public void process(OntologyEntryBean entry, String sourceName, OntologyConfiguration ontologyConfiguration) throws PluginException;

}
