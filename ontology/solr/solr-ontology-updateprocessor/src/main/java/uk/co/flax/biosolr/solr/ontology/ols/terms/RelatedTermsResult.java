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
package uk.co.flax.biosolr.solr.ontology.ols.terms;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * POJO wrapping the results of a related terms lookup, such as a child
 * or parent terms query.
 *
 * Created by mlp on 27/10/15.
 * @author mlp
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

	public List<OntologyTerm> getTerms() {
		return (embedded == null ? Collections.emptyList() : embedded.getTerms());
	}

	public Page getPage() {
		return page;
	}

}
