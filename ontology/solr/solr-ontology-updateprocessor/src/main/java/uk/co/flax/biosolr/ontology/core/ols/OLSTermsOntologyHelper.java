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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.ontology.core.OntologyHelperException;
import uk.co.flax.biosolr.ontology.core.ols.terms.OntologyTerm;
import uk.co.flax.biosolr.ontology.core.ols.terms.RelatedTermsResult;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	private Map<String, Set<RelatedTermsResult>> nonDefinitiveTerms = new HashMap<>();

	public OLSTermsOntologyHelper(String baseUrl) {
		super(baseUrl, null);
	}

	public OLSTermsOntologyHelper(String baseUrl, int pageSize, int threadpoolSize, ThreadFactory threadFactory) {
		super(baseUrl, null, pageSize, threadpoolSize, threadFactory);
	}

	@Override
	protected List<OntologyTerm> lookupTerms(final List<String> iris) throws OntologyHelperException {
		// Strip out the IRIs we already know to have non-definitive terms
		List<String> callIris = iris.stream()
				.filter(i -> !nonDefinitiveTerms.containsKey(i))
				.collect(Collectors.toList());
		List<Callable<RelatedTermsResult>> termsCalls = createTermsCalls(callIris);
		List<RelatedTermsResult> callResults = executeCalls(termsCalls);
		List<OntologyTerm> terms = new ArrayList<>(iris.size());

		Map<String, Set<RelatedTermsResult>> lookupMap = new HashMap<>();
		callResults.forEach(r -> {
			OntologyTerm t  = findDefinitiveTerm(r.getTerms());
			if (t == null) {
				if (r.isSinglePage()) {
					// Single page and non-definitive - add to the ndt list, resolve later
					nonDefinitiveTerms.put(getRelatedResultIri(r), Collections.singleton(r));
				} else {
					// Need to look up more pages - hold on to this result
					Set<RelatedTermsResult> lookupSet = new TreeSet<>(
							(RelatedTermsResult r1, RelatedTermsResult r2) -> r1.getPage().compareTo(r2.getPage()));
					lookupSet.add(r);
					lookupMap.put(getRelatedResultIri(r), lookupSet);
				}
			} else {
				terms.add(t);
			}
		});

		if (!lookupMap.isEmpty()) {
			List<Callable<RelatedTermsResult>> lookupCalls = new ArrayList<>(lookupMap.size());
			lookupMap.values().stream()
					.flatMap(Set::stream)
					.map(this::createRemainingTermsCalls)
					.forEach(lookupCalls::addAll);
			List<RelatedTermsResult> lookupResults = executeCalls(lookupCalls);
			lookupResults.forEach(r -> {
				if (lookupMap.containsKey(getRelatedResultIri(r)) && r.hasTerms()) {
					OntologyTerm t = findDefinitiveTerm(r.getTerms());
					if (t != null) {
						// Found the definitive term for this IRI
						terms.add(t);
						lookupMap.remove(t.getIri());
					} else {
						// Still non-definitive - add page to lookup map
						lookupMap.get(getRelatedResultIri(r)).add(r);
					}
				}
			});

			if (!lookupMap.isEmpty()) {
				// These are now all non-definitive - add to the cache
				nonDefinitiveTerms.putAll(lookupMap);
			}
		}

		// Now grab the first term for the non-definitive terms
		iris.stream()
				.filter(nonDefinitiveTerms::containsKey)
				.map(nonDefinitiveTerms::get)
				.forEach(s -> {
					OntologyTerm t = s.iterator().next().getFirstTerm();
					if (t != null) {
						terms.add(t);
					}
				});

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

		OntologyTerm t = result.getFirstTerm();
		if (t != null) {
			ret = t.getIri();
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
	private static OntologyTerm findDefinitiveTerm(List<OntologyTerm> terms) {
		OntologyTerm term = null;

		for (OntologyTerm t : terms) {
			if (t.isDefiningOntology()) {
				term = t;
				break;
			}
		}

		return term;
	}

}
