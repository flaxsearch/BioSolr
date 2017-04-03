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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * POJO wrapping the results of a related terms lookup, such as a child
 * or parent terms query.
 *
 * <p>Created by Matt Pearce on 27/10/15.</p>
 *
 * @author Matt Pearce
 */
public class RelatedTermsResult {

	private final Map<ResultsLinkType, Link> links;
	private final EmbeddedOntologyTerms embedded;
	private final Page page;

	public RelatedTermsResult(@JsonProperty("_links") Map<ResultsLinkType, Link> links,
							  @JsonProperty("_embedded") EmbeddedOntologyTerms embedded,
							  @JsonProperty("page") Page page) {
		this.links = links;
		this.embedded = embedded;
		this.page = page;
	}

	public Map<ResultsLinkType, Link> getLinks() {
		return links;
	}

	public EmbeddedOntologyTerms getEmbedded() {
		return embedded;
	}

	/**
	 * Get the terms held in the embedded ontology terms list.
	 * @return a list of terms. Never <code>null</code>.
	 */
	public List<OntologyTerm> getTerms() {
		return (embedded == null ? Collections.emptyList() : embedded.getTerms());
	}

	public Page getPage() {
		return page;
	}

	/**
	 * Is this a single page result?
	 * @return <code>true</code> if the page object indicates there is only one
	 * page, or there is no page information in the result.
	 */
	public boolean isSinglePage() {
		return page == null || page.getTotalPages() == 1;
	}

	/**
	 * @return <code>true</code> if this result contains any terms.
	 */
	public boolean hasTerms() {
		return embedded != null && embedded.getTerms() != null && !embedded.getTerms().isEmpty();
	}

}
