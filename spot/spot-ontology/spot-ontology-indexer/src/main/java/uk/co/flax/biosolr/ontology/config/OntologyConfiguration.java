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
package uk.co.flax.biosolr.ontology.config;

/**
 * @author Matt Pearce
 */
public class OntologyConfiguration {
	
	private String accessURI;
	
	private String synonymAnnotationURI;
	
	private String definitionAnnotationURI;
	
	private String obsoleteClassURI;

	/**
	 * @return the accessURI
	 */
	public String getAccessURI() {
		return accessURI;
	}

	/**
	 * @param accessURI the accessURI to set
	 */
	public void setAccessURI(String accessURI) {
		this.accessURI = accessURI;
	}

	/**
	 * @return the synonymAnnotationURI
	 */
	public String getSynonymAnnotationURI() {
		return synonymAnnotationURI;
	}

	/**
	 * @param synonymAnnotationURI the synonymAnnotationURI to set
	 */
	public void setSynonymAnnotationURI(String synonymAnnotationURI) {
		this.synonymAnnotationURI = synonymAnnotationURI;
	}

	/**
	 * @return the definitionAnnotationURI
	 */
	public String getDefinitionAnnotationURI() {
		return definitionAnnotationURI;
	}

	/**
	 * @param definitionAnnotationURI the definitionAnnotationURI to set
	 */
	public void setDefinitionAnnotationURI(String definitionAnnotationURI) {
		this.definitionAnnotationURI = definitionAnnotationURI;
	}

	/**
	 * @return the obsoleteClassURI
	 */
	public String getObsoleteClassURI() {
		return obsoleteClassURI;
	}

	/**
	 * @param obsoleteClassURI the obsoleteClassURI to set
	 */
	public void setObsoleteClassURI(String obsoleteClassURI) {
		this.obsoleteClassURI = obsoleteClassURI;
	}
	
}
