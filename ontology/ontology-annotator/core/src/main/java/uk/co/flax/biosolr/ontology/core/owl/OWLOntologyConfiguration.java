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

import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import uk.co.flax.biosolr.ontology.core.OntologyHelperConfiguration;

import java.util.List;

/**
 * Ontology configuration details for an OWL-based ontology.
 *
 * @author Matt Pearce
 */
public class OWLOntologyConfiguration extends OntologyHelperConfiguration {

	/** Default label property */
	public static final String LABEL_PROPERTY_URI = OWLRDFVocabulary.RDFS_LABEL.toString();
	/** Default synonym property */
	public static final String SYNONYM_PROPERTY_URI = "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym";
	/** Default definition property */
	public static final String DEFINITION_PROPERTY_URI = "http://purl.obolibrary.org/obo/IAO_0000115";
	
	private final String ontologyUri;

	private final List<String> labelPropertyUris;
	private final List<String> synonymPropertyUris;
	private final List<String> definitionPropertyUris;
	private final List<String> ignorePropertyUris;

	/**
	 * Build the OWL ontology configuration for an OWL OntologyHelper
	 * instance.
	 * @param ontologyUri the URI of the ontology.
	 * @param labelUris the label property or properties that should be used.
	 * @param synonymUris the synonym properties that should be used.
	 * @param definitionUris the definition properties that should be used.
	 * @param ignoreUris the properties that should be ignored. <b>Not used.</b>
	 */
	public OWLOntologyConfiguration(String ontologyUri, List<String> labelUris, List<String> synonymUris, List<String> definitionUris, List<String> ignoreUris) {
		this.ontologyUri = ontologyUri;
		this.labelPropertyUris = labelUris;
		this.synonymPropertyUris = synonymUris;
		this.definitionPropertyUris = definitionUris;
		this.ignorePropertyUris = ignoreUris;
	}

	/**
	 * @return the URI of the ontology.
	 */
	public String getOntologyUri() {
		return ontologyUri;
	}

	/**
	 * @return the label property or properties for this ontology.
	 */
	public List<String> getLabelPropertyUris() {
		return labelPropertyUris;
	}

	/**
	 * @return the synonym properties for this ontology.
	 */
	public List<String> getSynonymPropertyUris() {
		return synonymPropertyUris;
	}

	/**
	 * @return the definition properties for this ontology.
	 */
	public List<String> getDefinitionPropertyUris() {
		return definitionPropertyUris;
	}

	public List<String> getIgnorePropertyUris() {
		return ignorePropertyUris;
	}

}
