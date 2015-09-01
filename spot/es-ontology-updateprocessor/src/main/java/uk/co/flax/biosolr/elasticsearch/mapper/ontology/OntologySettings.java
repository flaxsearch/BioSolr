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

package uk.co.flax.biosolr.elasticsearch.mapper.ontology;

import java.util.Arrays;
import java.util.List;

import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * Ontology settings from the ontology mapping configuration.
 *
 * @author mlp
 */
public class OntologySettings {
	
	static final String ONTOLOGY_SETTINGS_KEY = "ontology";

	static final String ONTOLOGY_URI_PARAM = "ontologyURI";
	static final String LABEL_URI_PARAM = "labelURI";
	static final String SYNONYM_URI_PARAM = "synonymURI";
	static final String DEFINITION_URI_PARAM = "definitionURI";

	/*
	 * Default property annotation values.
	 */
	private static final String LABEL_PROPERTY_URI = OWLRDFVocabulary.RDFS_LABEL.toString();
	private static final String SYNONYM_PROPERTY_URI = "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym";
	private static final String DEFINITION_PROPERTY_URI = "http://purl.obolibrary.org/obo/IAO_0000115";

	private String ontologyUri;
	private List<String> labelPropertyUris = Arrays.asList(LABEL_PROPERTY_URI);
	private List<String> synonymPropertyUris = Arrays.asList(SYNONYM_PROPERTY_URI);
	private List<String> definitionPropertyUris = Arrays.asList(DEFINITION_PROPERTY_URI);

	public String getOntologyUri() {
		return ontologyUri;
	}

	public void setOntologyUri(String ontologyUri) {
		this.ontologyUri = ontologyUri;
	}

	public List<String> getLabelPropertyUris() {
		return labelPropertyUris;
	}

	public void setLabelPropertyUris(List<String> labelPropertyUris) {
		this.labelPropertyUris = labelPropertyUris;
	}

	public List<String> getSynonymPropertyUris() {
		return synonymPropertyUris;
	}

	public void setSynonymPropertyUris(List<String> synonymPropertyUris) {
		this.synonymPropertyUris = synonymPropertyUris;
	}

	public List<String> getDefinitionPropertyUris() {
		return definitionPropertyUris;
	}

	public void setDefinitionPropertyUris(List<String> definitionPropertyUris) {
		this.definitionPropertyUris = definitionPropertyUris;
	}

}
