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

/**
 * POJO wrapping terms returned in a {@link RelatedTermsResult} lookup
 * result.
 *
 * <p>Created by Matt Pearce on 27/10/15.</p>
 * @author Matt Pearce
 */
public class EmbeddedOntologyTerms {

	private final List<OntologyTerm> terms;

	public EmbeddedOntologyTerms(@JsonProperty("terms") List<OntologyTerm> terms) {
		this.terms = terms;
	}

	/**
	 * @return the terms returned from the {@link RelatedTermsResult} lookup.
	 */
	public List<OntologyTerm> getTerms() {
		return terms;
	}

}
