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
	
	public static final String LABEL_PROPERTY_KEY = "label_properties";
	public static final String SYNONYM_PROPERTY_KEY = "synonym_properties";
	public static final String DEFINITION_PROPERTY_KEY = "definition_properties";
	public static final String IGNORE_PROPERTY_KEY = "ignore_properties";

	private final List<String> labelPropertyUris;
	private final List<String> synonymPropertyUris;
	private final List<String> definitionPropertyUris;
	private final List<String> ignorePropertyUris;
	
	public OWLOntologyConfiguration(List<String> labelUris, List<String> synonymUris, List<String> definitionUris, List<String> ignoreUris) {
		this.labelPropertyUris = labelUris;
		this.synonymPropertyUris = synonymUris;
		this.definitionPropertyUris = definitionUris;
		this.ignorePropertyUris = ignoreUris;
	}

	/**
	 * Build an ontology configuration using sensible defaults.
	 * @return the default ontology configuration.
	 */
	public static OWLOntologyConfiguration defaultConfiguration() {
		return new OWLOntologyConfiguration(Collections.singletonList(LABEL_PROPERTY_URI),
				Collections.singletonList(SYNONYM_PROPERTY_URI),
				Collections.singletonList(DEFINITION_PROPERTY_URI),
				Collections.emptyList());
	}
	
	/**
	 * Build an ontology configuration using a properties file. Any missing
	 * properties will be replaced with the equivalent default value(s).
	 * @param propFile the properties file to read.
	 * @return the ontology configuration for the properties file.
	 * @throws IOException 
	 */
	public static OWLOntologyConfiguration fromPropertiesFile(String propFile) throws IOException {
		Path path = FileSystems.getDefault().getPath(propFile);
		Reader reader = Files.newBufferedReader(path);
		ResourceBundle rb = new PropertyResourceBundle(reader);
		
		String labels = getResourceString(rb, LABEL_PROPERTY_KEY, LABEL_PROPERTY_URI);
		String definitions = getResourceString( rb, DEFINITION_PROPERTY_KEY, DEFINITION_PROPERTY_URI);
		String synonyms = getResourceString(rb, SYNONYM_PROPERTY_KEY, SYNONYM_PROPERTY_URI);
		String ignores = getResourceString(rb, IGNORE_PROPERTY_KEY, "");
		
		List<String> labelUris = Arrays.asList(labels.split(",\\s*"));
		List<String> definitionUris = Arrays.asList(definitions.split(",\\s*"));
		List<String> synonymUris = Arrays.asList(synonyms.split(",\\s*"));
		List<String> ignoreUris = StringUtils.isNotBlank(ignores) ? Arrays.asList(ignores.split(",\\s*")) : Collections.emptyList();
		
		return new OWLOntologyConfiguration(labelUris, synonymUris, definitionUris, ignoreUris);
	}
	
	private static String getResourceString(ResourceBundle rb, String key, String defaultValue) {
		String ret;
		try {
			ret = rb.getString(key);
		} catch (MissingResourceException mre) {
			ret = defaultValue == null ? "" : defaultValue;
		}
		return ret;
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
