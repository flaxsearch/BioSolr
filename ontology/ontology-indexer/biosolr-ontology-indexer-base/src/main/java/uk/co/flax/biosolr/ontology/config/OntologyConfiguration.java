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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Matt Pearce
 */
public class OntologyConfiguration {
	
	private String source;
	
	private String namespace;
	
	private String accessURI;
	
	private String labelURI;
	
	private List<String> baseURI;
	
	private List<String> synonymAnnotationURI;
	
	private List<String> definitionAnnotationURI;
	
	private String obsoleteClassURI = "";
	
	private List<String> ignoreURIs = new ArrayList<>();
	
	private String reasoner;
	
	private int batchSize = 1000;

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
	public List<String> getSynonymAnnotationURI() {
		return synonymAnnotationURI;
	}

	/**
	 * @param synonymAnnotationURI the synonymAnnotationURI to set
	 */
	public void setSynonymAnnotationURI(List<String> synonymAnnotationURI) {
		this.synonymAnnotationURI = synonymAnnotationURI;
	}

	/**
	 * @return the definitionAnnotationURI
	 */
	public List<String> getDefinitionAnnotationURI() {
		return definitionAnnotationURI;
	}

	/**
	 * @param definitionAnnotationURI the definitionAnnotationURI to set
	 */
	public void setDefinitionAnnotationURI(List<String> definitionAnnotationURI) {
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

	/**
	 * @return the ignoreURIs
	 */
	public List<String> getIgnoreURIs() {
		return ignoreURIs;
	}

	/**
	 * @param ignoreURIs the ignoreURIs to set
	 */
	public void setIgnoreURIs(List<String> ignoreURIs) {
		this.ignoreURIs = ignoreURIs;
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * @return the reasoner
	 */
	public String getReasoner() {
		return reasoner;
	}

	/**
	 * @param reasoner the reasoner to set
	 */
	public void setReasoner(String reasoner) {
		this.reasoner = reasoner;
	}

	/**
	 * @return the batchSize
	 */
	public int getBatchSize() {
		return batchSize;
	}

	/**
	 * @param batchSize the batchSize to set
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	/**
	 * @return the labelURI
	 */
	public String getLabelURI() {
		return labelURI;
	}

	/**
	 * @param labelURI the labelURI to set
	 */
	public void setLabelURI(String labelURI) {
		this.labelURI = labelURI;
	}

	/**
	 * @return the namespace
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * @param namespace the namespace to set
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * @return the baseUris
	 */
	public List<String> getBaseURI() {
		return baseURI;
	}

	/**
	 * @param baseUris the baseUris to set
	 */
	public void setBaseURI(List<String> baseUris) {
		this.baseURI = baseUris;
	}
	
}
