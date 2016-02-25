/**
 * Copyright (c) 2016 Lemur Consulting Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.flax.biosolr.elasticsearch.mapper.ontology;

import java.util.*;

/**
 * Builder class for the OntologySettings object.
 *
 * Created by mlp on 26/01/16.
 * @author mlp
 */
public class OntologySettingsBuilder {

	private Map<String, Object> settingsNode;

	public OntologySettingsBuilder() {

	}

	public OntologySettingsBuilder settingsNode(Map<String, Object> node) {
		this.settingsNode = node;
		return this;
	}

	public OntologySettings build() {
		OntologySettings settings = new OntologySettings();

		for (Iterator<Map.Entry<String, Object>> iterator = settingsNode.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<String, Object> entry = iterator.next();
			String key = entry.getKey();
			if (entry.getValue() != null) {
				switch (key) {
					case OntologySettings.ONTOLOGY_URI_PARAM:
						settings.setOntologyUri(entry.getValue().toString());
						iterator.remove();
						break;
					case OntologySettings.LABEL_URI_PARAM:
						settings.setLabelPropertyUris(extractList(entry.getValue()));
						iterator.remove();
						break;
					case OntologySettings.SYNONYM_URI_PARAM:
						settings.setSynonymPropertyUris(extractList(entry.getValue()));
						iterator.remove();
						break;
					case OntologySettings.DEFINITION_URI_PARAM:
						settings.setDefinitionPropertyUris(extractList(entry.getValue()));
						iterator.remove();
						break;
					case OntologySettings.INCLUDE_INDIRECT_PARAM:
						settings.setIncludeIndirect(Boolean.parseBoolean(entry.getValue().toString()));
						iterator.remove();
						break;
					case OntologySettings.INCLUDE_RELATIONS_PARAM:
						settings.setIncludeRelations(Boolean.parseBoolean(entry.getValue().toString()));
						iterator.remove();
						break;
					case OntologySettings.OLS_BASE_URL_PARAM:
						settings.setOlsBaseUrl(entry.getValue().toString());
						iterator.remove();
						break;
					case OntologySettings.OLS_ONTOLOGY_PARAM:
						settings.setOlsOntology(entry.getValue().toString());
						iterator.remove();
						break;
					case OntologySettings.OLS_PAGESIZE_PARAM:
						settings.setPageSize(Integer.parseInt(entry.getValue().toString()));
						iterator.remove();
						break;
					case OntologySettings.OLS_THREADPOOL_PARAM:
						settings.setThreadpoolSize(Integer.parseInt(entry.getValue().toString()));
						iterator.remove();
						break;
					case OntologySettings.THREAD_CHECK_MS_PARAM:
						settings.setThreadCheckMs(Long.parseLong(entry.getValue().toString()));
						iterator.remove();
						break;
					case OntologySettings.INCLUDE_PARENT_PATHS_PARAM:
						settings.setIncludeParentPaths(Boolean.parseBoolean(entry.getValue().toString()));
						iterator.remove();
						break;
					case OntologySettings.INCLUDE_PARENT_PATH_LABELS_PARAM:
						settings.setIncludeParentPathLabels(Boolean.parseBoolean(entry.getValue().toString()));
						iterator.remove();
						break;
				}
			}
		}

		return settings;
	}

	@SuppressWarnings("rawtypes")
	private List<String> extractList(Object value) {
		List<String> ret = null;

		if (value instanceof String) {
			ret = Collections.singletonList((String) value);
		} else if (value instanceof List) {
			ret = new ArrayList<>(((List) value).size());
			for (Object v : (List) value) {
				ret.add(v.toString());
			}
		}

		return ret;
	}

}
