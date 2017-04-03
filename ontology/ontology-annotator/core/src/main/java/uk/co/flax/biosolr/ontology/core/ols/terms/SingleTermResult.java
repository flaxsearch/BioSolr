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

import java.util.Map;

/**
 * Result object wrapping the results for a single-term lookup.
 *
 * <p>Created by Matt Pearce on 03/12/15.</p>
 * @author Matt Pearce
 */
public class SingleTermResult extends RelatedTermsResult {

	private final String iri;
	private final boolean definitiveResult;

	public SingleTermResult(@JsonProperty("_links") Map<ResultsLinkType, Link> links,
							@JsonProperty("_embedded") EmbeddedOntologyTerms embedded,
							@JsonProperty("page") Page page) {
		super(links, embedded, page);
		this.iri = extractTermIri(embedded);
		this.definitiveResult = extractDefinitiveResult(embedded);
	}

	private String extractTermIri(EmbeddedOntologyTerms embedded) {
		String iri;

		if (hasTerms()) {
			OntologyTerm t = embedded.getTerms().get(0);
			iri = t.getIri();
		} else {
			iri = null;
		}

		return iri;
	}

	private boolean extractDefinitiveResult(EmbeddedOntologyTerms embedded) {
		boolean definitive = false;

		if (hasTerms()) {
			for (OntologyTerm t : embedded.getTerms()) {
				if (t.isDefiningOntology()) {
					definitive = true;
					break;
				}
			}
		}

		return definitive;
	}

	public String getIri() {
		return iri;
	}

	/**
	 * @return <code>true</code> if this result contains the defining ontology
	 * result for the term.
	 */
	public boolean isDefinitiveResult() {
		return definitiveResult;
	}

	/**
	 * Retrieve the ontology term from the defining ontology for this set of
	 * results.
	 * @return the term, or <code>null</code> if this set of results does not contain
	 * a term from the defining ontology.
	 */
	public OntologyTerm getDefinitiveResult() {
		OntologyTerm term = null;

		if (isDefinitiveResult()) {
			for (OntologyTerm t : getEmbedded().getTerms()) {
				if (t.isDefiningOntology()) {
					term = t;
					break;
				}
			}
		}

		return term;
	}

	/**
	 * Retrieve the first term from this set of results, if available.
	 * @return the first term, or <code>null</code> if no results available.
	 */
	public OntologyTerm getFirstTerm() {
		OntologyTerm t = null;

		if (hasTerms()) {
			t = getEmbedded().getTerms().get(0);
		}

		return t;
	}

}
