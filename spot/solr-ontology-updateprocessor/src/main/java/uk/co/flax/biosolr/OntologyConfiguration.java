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

package uk.co.flax.biosolr;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * JavaDoc for OntologyConfiguration.
 *
 * @author mlp
 */
public class OntologyConfiguration {
	
	public static final String LABEL_PROPERTY_URI = OWLRDFVocabulary.RDFS_LABEL.toString();
	public static final String SYNONYM_PROPERTY_URI = "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym";
	public static final String DEFINITION_PROPERTY_URI = "http://purl.obolibrary.org/obo/IAO_0000115";

	private List<String> labelPropertyUris;
	private List<String> synonymPropertyUris;
	private List<String> definitionPropertyUris;
	private List<String> obsoletePropertyUris;
	
	/**
	 * Build an ontology configuration using sensible defaults.
	 * @return the default ontology configuration.
	 */
	public static OntologyConfiguration defaultConfiguration() {
		OntologyConfiguration config = new OntologyConfiguration();
		config.setLabelPropertyUris(Arrays.asList(LABEL_PROPERTY_URI));
		config.setSynonymPropertyUris(Arrays.asList(SYNONYM_PROPERTY_URI));
		config.setDefinitionPropertyUris(Arrays.asList(DEFINITION_PROPERTY_URI));
		config.setObsoletePropertyUris(Collections.emptyList());
		return config;
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

	public List<String> getObsoletePropertyUris() {
		return obsoletePropertyUris;
	}

	public void setObsoletePropertyUris(List<String> obsoletePropertyUris) {
		this.obsoletePropertyUris = obsoletePropertyUris;
	}

}
