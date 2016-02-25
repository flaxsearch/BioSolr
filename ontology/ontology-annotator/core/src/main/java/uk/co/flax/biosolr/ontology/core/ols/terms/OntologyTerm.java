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
 *
 * <p>Created by Matt Pearce on 21/10/15.</p>
 *
 * @author Matt Pearce
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
	private final Map<String, Link> links;

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
						@JsonProperty("_links") Map<String, Link> links) {
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

	/**
	 * @return the IRI of the term.
	 */
	public String getIri() {
		return iri;
	}

	/**
	 * @return the label.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the description(s).
	 */
	public List<String> getDescription() {
		return description;
	}

	/**
	 * @return the synonym(s) for the term.
	 */
	public List<String> getSynonyms() {
		return synonyms;
	}

	/**
	 * @return the name of the ontology from which this term was retrieved.
	 */
	public String getOntologyName() {
		return ontologyName;
	}

	/**
	 * @return the prefix of the ontology - eg. EFO, GO.
	 */
	public String getOntologyPrefix() {
		return ontologyPrefix;
	}

	/**
	 * @return the root IRI for the ontology.
	 */
	public String getOntologyIri() {
		return ontologyIri;
	}

	/**
	 * @return <code>true</code> if this node is obsolete.
	 */
	public boolean isObsolete() {
		return obsolete;
	}

	/**
	 * Check if the ontology is the defining ontology for the term,
	 * or if it contains the term from another ontology.
	 * @return <code>true</code> if the ontology defines this term.
	 */
	public boolean isDefiningOntology() {
		return definingOntology;
	}

	/**
	 * @return <code>true</code> if the term has children.
	 */
	public boolean isHasChildren() {
		return hasChildren;
	}

	/**
	 * @return if the term is a root node in the ontology - eg.
	 * OWL#Thing.
	 */
	public boolean isRoot() {
		return root;
	}

	/**
	 * @return the short form of the term's IRI.
	 */
	public String getShortForm() {
		return shortForm;
	}

	/**
	 * The links hold URLs allowing parents, children, and other related
	 * terms to be looked up. The link types are a super-set of those defined
	 * in {@link TermLinkType}.
	 * @return the map of links.
	 */
	public Map<String, Link> getLinks() {
		return links;
	}
}
