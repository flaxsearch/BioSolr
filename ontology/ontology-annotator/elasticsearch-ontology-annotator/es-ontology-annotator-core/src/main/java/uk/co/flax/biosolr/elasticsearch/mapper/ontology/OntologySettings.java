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

import uk.co.flax.biosolr.ontology.core.ols.OLSOntologyHelper;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ontology settings from the ontology mapping configuration.
 *
 * @author mlp
 */
public class OntologySettings {

	public static final long DELETE_CHECK_DELAY_MS = 15 * 60 * 1000; // 15 minutes

	static final String ONTOLOGY_SETTINGS_KEY = "ontology";

	// OWL parameters
	static final String ONTOLOGY_URI_PARAM = "ontologyURI";
	static final String LABEL_URI_PARAM = "labelURI";
	static final String SYNONYM_URI_PARAM = "synonymURI";
	static final String DEFINITION_URI_PARAM = "definitionURI";

	// OLS parameters
	static final String OLS_BASE_URL_PARAM = "olsBaseURL";
	static final String OLS_ONTOLOGY_PARAM = "olsOntology";
	static final String OLS_THREADPOOL_PARAM = "olsThreadpool";
	static final String OLS_PAGESIZE_PARAM = "olsPageSize";

	static final String INCLUDE_INDIRECT_PARAM = "includeIndirect";
	static final String INCLUDE_RELATIONS_PARAM = "includeRelations";

	static final String INCLUDE_PARENT_PATHS_PARAM = "includeParentPaths";
	static final String INCLUDE_PARENT_PATH_LABELS_PARAM = "includeParentPathLabels";

	static final String THREAD_CHECK_MS_PARAM = "threadCheckMs";

	private String ontologyUri;
	private List<String> labelPropertyUris = Collections.singletonList(OWLOntologyConfiguration.LABEL_PROPERTY_URI);
	private List<String> synonymPropertyUris = Collections.singletonList(OWLOntologyConfiguration.SYNONYM_PROPERTY_URI);
	private List<String> definitionPropertyUris = Collections.singletonList(OWLOntologyConfiguration.DEFINITION_PROPERTY_URI);
	private boolean includeIndirect = true;
	private boolean includeRelations = true;
	private boolean includeParentPaths = false;
	private boolean includeParentPathLabels = false;

	private String olsBaseUrl;
	private String olsOntology;
	private int threadpoolSize = OLSOntologyHelper.THREADPOOL_SIZE;
	private int pageSize = OLSOntologyHelper.PAGE_SIZE;

	private long threadCheckMs = DELETE_CHECK_DELAY_MS;

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

	public boolean isIncludeIndirect() {
		return includeIndirect;
	}

	public void setIncludeIndirect(boolean includeIndirect) {
		this.includeIndirect = includeIndirect;
	}

	public boolean isIncludeRelations() {
		return includeRelations;
	}

	public void setIncludeRelations(boolean includeRelations) {
		this.includeRelations = includeRelations;
	}

	public String getOlsBaseUrl() {
		return olsBaseUrl;
	}

	public void setOlsBaseUrl(String olsBaseUrl) {
		this.olsBaseUrl = olsBaseUrl;
	}

	public String getOlsOntology() {
		return olsOntology;
	}

	public void setOlsOntology(String olsOntology) {
		this.olsOntology = olsOntology;
	}

	public int getThreadpoolSize() {
		return threadpoolSize;
	}

	public void setThreadpoolSize(int threadpoolSize) {
		this.threadpoolSize = threadpoolSize;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public long getThreadCheckMs() {
		return threadCheckMs;
	}

	public void setThreadCheckMs(long threadCheckMs) {
		this.threadCheckMs = threadCheckMs;
	}

	public boolean isIncludeParentPaths() {
		return includeParentPaths;
	}

	public void setIncludeParentPaths(boolean includeParentPaths) {
		this.includeParentPaths = includeParentPaths;
	}

	public boolean isIncludeParentPathLabels() {
		return includeParentPathLabels;
	}

	public void setIncludeParentPathLabels(boolean includeParentPathLabels) {
		this.includeParentPathLabels = includeParentPathLabels;
	}

	/**
	 * Get a list of the default field mappings appropriate for this ontology mapper. This
	 * will exclude the ancestor and descendant mappings if includeIndirect is <code>false</code>.
	 * @return the list of field mappings.
	 */
	public List<FieldMappings> getFieldMappings() {
		// Assume we need all the mappings
		List<FieldMappings> mappingsList = new ArrayList<>(FieldMappings.values().length);

		for (FieldMappings fm : FieldMappings.values()) {
			if (!includeIndirect && fm.isIndirect()) {
				continue;
			} else if (fm == FieldMappings.PARENT_PATHS && !includeParentPaths) {
				continue;
			}
			mappingsList.add(fm);
		}

		return mappingsList;
	}

}
