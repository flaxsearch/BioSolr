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

	public void setOntologyUrl(String ontologyUrl) {
		this.ontologyUrl = ontologyUrl;
	}

	public String getOntologyRequestHandler() {
		return ontologyRequestHandler;
	}

	public void setOntologyRequestHandler(String ontologyRequestHandler) {
		this.ontologyRequestHandler = ontologyRequestHandler;
	}

	public String getOntologyNodeRequestHandler() {
		return ontologyNodeRequestHandler;
	}

	public void setOntologyNodeRequestHandler(String ontologyNodeRequestHandler) {
		this.ontologyNodeRequestHandler = ontologyNodeRequestHandler;
	}

	public String getDocumentUrl() {
		return documentUrl;
	}

	public void setDocumentUrl(String documentUrl) {
		this.documentUrl = documentUrl;
	}

	public String getDocumentRequestHandler() {
		return documentRequestHandler;
	}

	public void setDocumentRequestHandler(String documentRequestHandler) {
		this.documentRequestHandler = documentRequestHandler;
	}

	public String getDocumentUriRequestHandler() {
		return documentUriRequestHandler;
	}

	public void setDocumentUriRequestHandler(String documentUriRequestHandler) {
		this.documentUriRequestHandler = documentUriRequestHandler;
	}

	public int getOntologyTermCount() {
		return ontologyTermCount;
	}

	public void setOntologyTermCount(int ontologyTermCount) {
		this.ontologyTermCount = ontologyTermCount;
	}

	public List<String> getFacetFields() {
		return facetFields;
	}

	public void setFacetFields(List<String> facetFields) {
		this.facetFields = facetFields;
	}

	public FacetTreeConfiguration getDocumentFacetTree() {
		return documentFacetTree;
	}

	public void setDocumentFacetTree(FacetTreeConfiguration documentFacetTree) {
		this.documentFacetTree = documentFacetTree;
	}
}
