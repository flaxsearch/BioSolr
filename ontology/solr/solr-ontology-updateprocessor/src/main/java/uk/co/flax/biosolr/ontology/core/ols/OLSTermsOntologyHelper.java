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
package uk.co.flax.biosolr.ontology.core.ols;

import org.apache.hadoop.ipc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.ontology.core.OntologyHelperException;
import uk.co.flax.biosolr.ontology.core.ols.terms.EmbeddedOntologyTerms;
import uk.co.flax.biosolr.ontology.core.ols.terms.OntologyTerm;
import uk.co.flax.biosolr.ontology.core.ols.terms.RelatedTermsResult;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

/**
 * OLS-specific ontology helper implementation, handling the case
 * where the user has not given the ontology. In this case, all
 * terms lookups are searches, and need to be handled slightly
 * differently.
 *
 * Created by mlp on 10/11/15.
 * @author mlp
 */
public class OLSTermsOntologyHelper extends OLSOntologyHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(OLSTermsOntologyHelper.class);

	public OLSTermsOntologyHelper(String baseUrl) {
		super(baseUrl, null);
	}

	public OLSTermsOntologyHelper(String baseUrl, int pageSize, int threadpoolSize, ThreadFactory threadFactory) {
		super(baseUrl, null, pageSize, threadpoolSize, threadFactory);
	}

	@Override
	protected List<OntologyTerm> lookupTerms(final List<String> iris) throws OntologyHelperException {
		List<Callable<RelatedTermsResult>> termsCalls = createTermsCalls(iris);
		List<RelatedTermsResult> callResults = executeCalls(termsCalls);
		List<OntologyTerm> terms = new ArrayList<>(iris.size());

		Map<String, RelatedTermsResult> lookupMap = new HashMap<>();
		List<RelatedTermsResult> lookups = new ArrayList<>(callResults.size());
		callResults.forEach(r -> {
			if (r.getPage().getTotalPages() > 1) {
				OntologyTerm t = findDefinitiveTerm(r.getTerms(), true);
				if (t == null) {
					// Need to look up more pages - hold on to this result
					lookups.add(r);
					lookupMap.put(getRelatedResultIri(r), r);
				} else {
					terms.add(t);
				}
			} else if (!r.getTerms().isEmpty()) {
				terms.add(findDefinitiveTerm(r.getTerms(), false));
			}
		});

		if (!lookups.isEmpty()) {
			List<Callable<RelatedTermsResult>> lookupCalls = new ArrayList<>(lookups.size());
			lookups.forEach(l -> lookupCalls.addAll(createRemainingTermsCalls(l)));
			List<RelatedTermsResult> lookupResults = executeCalls(lookupCalls);
			lookupResults.forEach(r -> {
				if (lookupMap.containsKey(getRelatedResultIri(r)) && !r.getTerms().isEmpty()) {
					OntologyTerm t = findDefinitiveTerm(r.getTerms(), true);
					if (t != null) {
						// Found the definitive term for this IRI
						terms.add(t);
						lookupMap.remove(t.getIri());
					}
				}
			});

			if (!lookupMap.isEmpty()) {
				// Take first result for remaining entries
				lookupMap.values().forEach(r -> terms.add(findDefinitiveTerm(r.getTerms(), false)));
			}
		}

		return terms;
	}

	/**
	 * Build a list of callable requests to look up IRI terms.
	 * @param iris the IRIs to look up.
	 * @return a list of Callable requests.
	 */
	private List<Callable<RelatedTermsResult>> createTermsCalls(List<String> iris) {
		// Build a list of URLs we need to call
		List<String> urls = new ArrayList<>(iris.size());
		for (final String iri : iris) {
			try {
				final String termUrl = buildTermUrl(iri);
				urls.addAll(buildPageUrls(termUrl, 0, 1));
			} catch (UnsupportedEncodingException e) {
				// Not expecting to get here
				LOGGER.error(e.getMessage());
			}
		}
		return createCalls(urls, RelatedTermsResult.class);
	}

	private String getRelatedResultIri(RelatedTermsResult result) {
		String ret = null;

		if (!result.getTerms().isEmpty()) {
			ret = result.getTerms().get(0).getIri();
		}

		return ret;
	}

	private String buildTermUrl(String iri) throws UnsupportedEncodingException {
		final String dblEncodedIri = URLEncoder.encode(URLEncoder.encode(iri, ENCODING), ENCODING);
		return getBaseUrl() + TERMS_URL_SUFFIX + "/" + dblEncodedIri;
	}

	private List<Callable<RelatedTermsResult>> createRemainingTermsCalls(RelatedTermsResult result) {
		List<Callable<RelatedTermsResult>> calls;

		try {
			if (result.getTerms() == null || result.getTerms().isEmpty()) {
				calls = Collections.emptyList();
			} else {
				final String termUrl = buildTermUrl(getRelatedResultIri(result));
				calls = createCalls(buildPageUrls(termUrl, 1, result.getPage().getTotalPages()), RelatedTermsResult.class);
			}
		} catch (UnsupportedEncodingException e) {
			// Not expecting to get here
			LOGGER.error(e.getMessage());
			calls = Collections.emptyList();
		}

		return calls;
	}

	/**
	 * Run through a list of OntologyTerm items, extracted from an embedded
	 * ontology term results object, finding the
	 * definitive definition of the term. This is defined as <em>either</em>
	 * the first term found where <code>is_defining_ontology</code> is true,
	 * or the first returned term, if there is no term from the defining
	 * ontology.
	 * @param terms the embedded ontology term results object.
	 * @return the definitive OntologyTerm, or <code>null</code> if none can
	 * be found (this should only happen if the results list is empty).
	 */
	private static OntologyTerm findDefinitiveTerm(List<OntologyTerm> terms, boolean defining) {
		OntologyTerm term = null;

		for (OntologyTerm t : terms) {
			if (t.isDefiningOntology()) {
				term = t;
				break;
			}
		}

		if (term == null && !defining) {
			term = terms.get(0);
		}

		return term;
	}

}
