/**
 * Copyright (c) 2015 Lemur Consulting Ltd.
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
package uk.co.flax.biosolr.ontology.core.ols.terms;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Object holding ontology terms, as read from OLS.
 * <p>
 * Created by mlp on 21/10/15.
 *
 * @author mlp
 */
public class OntologyTerm {

	private final String iri;
	private final String label;
	private final List<String> description;
	private final List<String> synonyms;
	private final String ontologyName;
	private final String ontologyPrefix;
	private final String ontologyIri;
	private final boolean obsolete;
	private final boolean definingOntology;
	private final boolean hasChildren;
	private final boolean root;
	private final String shortForm;
	private final Map<TermLinkType, Link> links;

	public OntologyTerm(@JsonProperty("iri") String iri,
						@JsonProperty("label") String label,
						@JsonProperty("description") List<String> description,
						@JsonProperty("synonyms") List<String> synonyms,
						@JsonProperty("ontology_name") String ontologyName,
						@JsonProperty("ontology_prefix") String ontologyPrefix,
						@JsonProperty("ontology_iri") String ontologyIri,
						@JsonProperty("is_obsolete") boolean obsolete,
						@JsonProperty("is_defining_ontology") boolean definingOntology,
						@JsonProperty("has_children") boolean hasChildren,
						@JsonProperty("is_root") boolean root,
						@JsonProperty("short_form") String shortForm,
						@JsonProperty("_links") Map<TermLinkType, Link> links) {
		this.iri = iri;
		this.label = label;
		this.description = description;
		this.synonyms = synonyms;
		this.ontologyName = ontologyName;
		this.ontologyPrefix = ontologyPrefix;
		this.ontologyIri = ontologyIri;
		this.obsolete = obsolete;
		this.definingOntology = definingOntology;
		this.hasChildren = hasChildren;
		this.root = root;
		this.shortForm = shortForm;
		this.links = links;
	}

	public String getIri() {
		return iri;
	}

	public String getLabel() {
		return label;
	}

	public List<String> getDescription() {
		return description;
	}

	public List<String> getSynonyms() {
		return synonyms;
	}

	public String getOntologyName() {
		return ontologyName;
	}

	public String getOntologyPrefix() {
		return ontologyPrefix;
	}

	public String getOntologyIri() {
		return ontologyIri;
	}

	public boolean isObsolete() {
		return obsolete;
	}

	public boolean isDefiningOntology() {
		return definingOntology;
	}

	public boolean isHasChildren() {
		return hasChildren;
	}

	public boolean isRoot() {
		return root;
	}

	public String getShortForm() {
		return shortForm;
	}

	public Map<TermLinkType, Link> getLinks() {
		return links;
	}
}
