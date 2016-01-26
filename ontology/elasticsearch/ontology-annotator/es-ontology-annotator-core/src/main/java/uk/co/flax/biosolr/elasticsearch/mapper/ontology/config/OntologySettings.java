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

package uk.co.flax.biosolr.elasticsearch.mapper.ontology.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import uk.co.flax.biosolr.elasticsearch.mapper.ontology.FieldMappings;
import uk.co.flax.biosolr.ontology.core.ols.OLSOntologyHelper;

/**
 * Ontology settings from the ontology mapping configuration.
 *
 * @author mlp
 */
public class OntologySettings {

	private static final long DELETE_CHECK_DELAY_MS = 15 * 60 * 1000; // 15 minutes

	public static final String ONTOLOGY_SETTINGS_KEY = "ontology";

	/*
	 * Default property annotation values.
	 */
	private static final String LABEL_PROPERTY_URI = OWLRDFVocabulary.RDFS_LABEL.toString();
	private static final String SYNONYM_PROPERTY_URI = "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym";
	private static final String DEFINITION_PROPERTY_URI = "http://purl.obolibrary.org/obo/IAO_0000115";

	private String ontologyUri;
	private List<String> labelPropertyUris = Collections.singletonList(LABEL_PROPERTY_URI);
	private List<String> synonymPropertyUris = Collections.singletonList(SYNONYM_PROPERTY_URI);
	private List<String> definitionPropertyUris = Collections.singletonList(DEFINITION_PROPERTY_URI);
	private boolean includeIndirect = true;
	private boolean includeRelations = true;

	private String olsBaseUrl;
	private String olsOntology;
	private int threadpoolSize = OLSOntologyHelper.THREADPOOL_SIZE;
	private int pageSize = OLSOntologyHelper.PAGE_SIZE;

	private long threadCheckMs = DELETE_CHECK_DELAY_MS;

	public String getOntologyUri() {
		return ontologyUri;
	}

	void setOntologyUri(String ontologyUri) {
		this.ontologyUri = ontologyUri;
	}

	public List<String> getLabelPropertyUris() {
		return labelPropertyUris;
	}

	void setLabelPropertyUris(List<String> labelPropertyUris) {
		this.labelPropertyUris = labelPropertyUris;
	}

	public List<String> getSynonymPropertyUris() {
		return synonymPropertyUris;
	}

	void setSynonymPropertyUris(List<String> synonymPropertyUris) {
		this.synonymPropertyUris = synonymPropertyUris;
	}

	public List<String> getDefinitionPropertyUris() {
		return definitionPropertyUris;
	}

	void setDefinitionPropertyUris(List<String> definitionPropertyUris) {
		this.definitionPropertyUris = definitionPropertyUris;
	}

	public boolean isIncludeIndirect() {
		return includeIndirect;
	}

	void setIncludeIndirect(boolean includeIndirect) {
		this.includeIndirect = includeIndirect;
	}

	public boolean isIncludeRelations() {
		return includeRelations;
	}

	void setIncludeRelations(boolean includeRelations) {
		this.includeRelations = includeRelations;
	}

	public String getOlsBaseUrl() {
		return olsBaseUrl;
	}

	void setOlsBaseUrl(String olsBaseUrl) {
		this.olsBaseUrl = olsBaseUrl;
	}

	public String getOlsOntology() {
		return olsOntology;
	}

	void setOlsOntology(String olsOntology) {
		this.olsOntology = olsOntology;
	}

	public int getThreadpoolSize() {
		return threadpoolSize;
	}

	void setThreadpoolSize(int threadpoolSize) {
		this.threadpoolSize = threadpoolSize;
	}

	public int getPageSize() {
		return pageSize;
	}

	void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public long getThreadCheckMs() {
		return threadCheckMs;
	}

	void setThreadCheckMs(long threadCheckMs) {
		this.threadCheckMs = threadCheckMs;
	}

	/**
	 * Get a list of the default field mappings appropriate for this ontology mapper. This
	 * will exclude the ancestor and descendant mappings if includeIndirect is <code>false</code>.
	 * @return the list of field mappings.
	 */
	public List<FieldMappings> getFieldMappings() {
		// Assume we need all the mappings
		List<FieldMappings> mappingList = Arrays.asList(FieldMappings.values());
		if (!includeIndirect) {
			// Don't need indirect mappings - remove them
			mappingList.removeAll(
					Arrays.asList(FieldMappings.ANCESTOR_LABEL, FieldMappings.ANCESTOR_URI,
							FieldMappings.DESCENDANT_LABEL, FieldMappings.DESCENDANT_URI));
		}
		return mappingList;
	}

}
