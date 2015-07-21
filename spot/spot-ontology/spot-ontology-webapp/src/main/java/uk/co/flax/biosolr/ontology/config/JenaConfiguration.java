/**
 * Copyright (c) 2014 Lemur Consulting Ltd.
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
package uk.co.flax.biosolr.ontology.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration details specific to Apache Jena.
 * 
 * @author Matt Pearce
 */
public class JenaConfiguration {
	
	@JsonProperty("ontologyUri")
	private String ontologyUri = null;
	
	@JsonProperty("tdbPath")
	private String tdbPath = null;
	
	private String entityField;
	private String primaryField;
	
	private Map<String, List<String>> fieldMappings;
	
	private String assemblerFile;
	private String assemblerDataset;

	/**
	 * @return the ontologyUri
	 */
	public String getOntologyUri() {
		return ontologyUri;
	}
	
	public String getTdbPath() {
		return tdbPath;
	}

	public String getPrimaryField() {
		return primaryField;
	}

	public Map<String, List<String>> getFieldMappings() {
		return fieldMappings;
	}

	public String getAssemblerFile() {
		return assemblerFile;
	}

	public String getAssemblerDataset() {
		return assemblerDataset;
	}

	public String getEntityField() {
		return entityField;
	}

}
