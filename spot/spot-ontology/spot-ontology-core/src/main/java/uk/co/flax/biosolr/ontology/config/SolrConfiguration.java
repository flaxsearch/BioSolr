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

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for the Solr search engines.
 * 
 * @author Matt Pearce
 */
public class SolrConfiguration {
	
	@NotNull @Valid
	private String ontologyUrl;
	
	private String ontologyRequestHandler;
	
	private String ontologyNodeRequestHandler;
	
	@NotNull @Valid
	private String documentUrl;
	
	private String documentRequestHandler;
	
	private String documentUriRequestHandler;
	
	private int ontologyTermCount;
	
	private List<String> facetFields = new ArrayList<>();
	
	@JsonProperty("documentFacetTree")
	private FacetTreeConfiguration documentFacetTree;
	
	public String getOntologyUrl() {
		return ontologyUrl;
	}
	
	public String getOntologyRequestHandler() {
		return ontologyRequestHandler;
	}
	
	public String getDocumentUrl() {
		return documentUrl;
	}

	/**
	 * @return the documentRequestHandler
	 */
	public String getDocumentRequestHandler() {
		return documentRequestHandler;
	}

	/**
	 * @return the documentUriRequestHandler
	 */
	public String getDocumentUriRequestHandler() {
		return documentUriRequestHandler;
	}

	/**
	 * @return the ontologyTermCount
	 */
	public int getOntologyTermCount() {
		return ontologyTermCount;
	}

	/**
	 * @return the facetFields
	 */
	public List<String> getFacetFields() {
		return facetFields;
	}

	/**
	 * @return the ontologyNodeRequestHandler
	 */
	public String getOntologyNodeRequestHandler() {
		return ontologyNodeRequestHandler;
	}

	public FacetTreeConfiguration getDocumentFacetTree() {
		return documentFacetTree;
	}

}
