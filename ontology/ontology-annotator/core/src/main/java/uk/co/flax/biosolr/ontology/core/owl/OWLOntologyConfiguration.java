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

package uk.co.flax.biosolr.ontology.core.owl;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import uk.co.flax.biosolr.ontology.core.OntologyHelperConfiguration;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Ontology configuration details, as read from a properties file
 * for each required ontology.
 *
 * @author mlp
 */
public class OWLOntologyConfiguration extends OntologyHelperConfiguration {
	
	public static final String LABEL_PROPERTY_URI = OWLRDFVocabulary.RDFS_LABEL.toString();
	public static final String SYNONYM_PROPERTY_URI = "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym";
	public static final String DEFINITION_PROPERTY_URI = "http://purl.obolibrary.org/obo/IAO_0000115";
	
	private final String ontologyUri;

	private final List<String> labelPropertyUris;
	private final List<String> synonymPropertyUris;
	private final List<String> definitionPropertyUris;
	private final List<String> ignorePropertyUris;
	
	public OWLOntologyConfiguration(String ontologyUri, List<String> labelUris, List<String> synonymUris, List<String> definitionUris, List<String> ignoreUris) {
		this.ontologyUri = ontologyUri;
		this.labelPropertyUris = labelUris;
		this.synonymPropertyUris = synonymUris;
		this.definitionPropertyUris = definitionUris;
		this.ignorePropertyUris = ignoreUris;
	}

	public String getOntologyUri() {
		return ontologyUri;
	}

	public List<String> getLabelPropertyUris() {
		return labelPropertyUris;
	}

	public List<String> getSynonymPropertyUris() {
		return synonymPropertyUris;
	}

	public List<String> getDefinitionPropertyUris() {
		return definitionPropertyUris;
	}

	public List<String> getIgnorePropertyUris() {
		return ignorePropertyUris;
	}

}
