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
import uk.co.flax.biosolr.ontology.core.ols.terms.SingleTermResult;
import uk.co.flax.biosolr.ontology.core.ols.terms.TermLinkType;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OLS-specific ontology helper implementation, handling the case
 * where the user has not given the ontology. In this case, all
 * terms lookups are searches, and need to be handled slightly
 * differently.
 *
 * <p>Created by Matt Pearce on 10/11/15.</p>
 * @author Matt Pearce
 */
public class OLSTermsOntologyHelper extends OLSOntologyHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(OLSTermsOntologyHelper.class);

	// Cache of terms with no defining ontology
	private final Map<String, Set<SingleTermResult>> nonDefinitiveTerms = new HashMap<>();

	public OLSTermsOntologyHelper(OLSOntologyConfiguration config, OLSHttpClient olsClient) {
		super(config, olsClient);
	}

	@Override
	protected List<OntologyTerm> lookupTerms(final List<String> iris) throws OntologyHelperException {
		// Strip out the IRIs we already know to have non-definitive terms
		Set<String> callIris = iris.stream()
				.filter(i -> !nonDefinitiveTerms.containsKey(i))
				.collect(Collectors.toSet());
		Collection<String> termsUrls = createTermsURLs(callIris, 0, 1);
		List<SingleTermResult> callResults = olsClient.callOLS(termsUrls, SingleTermResult.class);
		List<OntologyTerm> terms = new ArrayList<>(iris.size());

		Map<String, Set<SingleTermResult>> lookupMap = new HashMap<>();
		callResults.stream().filter(SingleTermResult::hasTerms).forEach(r -> {
			if (r.isDefinitiveResult()) {
				terms.add(r.getDefinitiveResult());
			} else if (r.isSinglePage()) {
				// Single page and non-definitive - add to the ndt list, resolve later
				nonDefinitiveTerms.put(r.getIri(), Collections.singleton(r));
			} else {
				// Need to look up more pages - hold on to this result
				Set<SingleTermResult> lookupSet = new TreeSet<>(
						(SingleTermResult r1, SingleTermResult r2) -> r1.getPage().compareTo(r2.getPage()));
				lookupSet.add(r);
				lookupMap.put(r.getIri(), lookupSet);
			}
		});

		if (!lookupMap.isEmpty()) {
			List<String> lookupUrls = new ArrayList<>(lookupMap.size());
			lookupMap.values().stream().flatMap(Set::stream)
					.map(r -> createTermsURLs(Collections.singletonList(r.getIri()), 1, r.getPage().getTotalPages()))
					.forEach(lookupUrls::addAll);
			List<SingleTermResult> lookupResults = olsClient.callOLS(lookupUrls, SingleTermResult.class);
			lookupResults.stream()
					.filter(SingleTermResult::hasTerms)
					.forEach(r -> {
						if (lookupMap.containsKey(r.getIri()) && r.isDefinitiveResult()) {
							terms.add(r.getDefinitiveResult());
							lookupMap.remove(r.getIri());
						} else {
							// Still non-definitive - add page to lookup map
							lookupMap.get(r.getIri()).add(r);
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
				.forEach(iri -> {
					SingleTermResult r = nonDefinitiveTerms.get(iri).iterator().next();
					if (r.hasTerms()) {
						terms.add(r.getFirstTerm());
					}
				});

		return terms;
	}

	private Collection<String> createTermsURLs(Collection<String> iris, int startPage, int endPage) {
		// Build a list of URLs we need to call
		List<String> urls = new ArrayList<>(iris.size());
		for (final String iri : iris) {
			try {
				final String termUrl = buildTermUrl(iri);
				urls.addAll(buildPageUrls(termUrl, startPage, endPage));
			} catch (UnsupportedEncodingException e) {
				// Not expecting to get here
				LOGGER.error(e.getMessage());
			}
		}
		return urls;
	}

	/**
	 * Build the base URL for searching for a particular term.
	 * @param iri the term to look up.
	 * @return the URL.
	 * @throws UnsupportedEncodingException if the default encoding (UTF-8) is
	 * not supported.
	 */
	private String buildTermUrl(String iri) throws UnsupportedEncodingException {
		// IRI is double encoded in the URL
		final String dblEncodedIri = URLEncoder.encode(URLEncoder.encode(iri, ENCODING), ENCODING);
		return getBaseUrl() + TERMS_URL_SUFFIX + "/" + dblEncodedIri;
	}

	@Override
	public Collection<String> getParentIris(String iri) throws OntologyHelperException {
		Collection<String> parents;

		checkTerm(iri);
		if (!nonDefinitiveTerms.containsKey(iri)) {
			parents = super.getParentIris(iri);
		} else {
			// This IRI has no defining ontology - look up parents for all found terms
			parents = findRelatedNonDefinitiveTerms(iri, TermLinkType.PARENTS);
		}

		return parents;
	}

	@Override
	public Collection<String> getAncestorIris(String iri) throws OntologyHelperException {
		Collection<String> parents;

		checkTerm(iri);
		if (!nonDefinitiveTerms.containsKey(iri)) {
			parents = super.getParentIris(iri);
		} else {
			// This IRI has no defining ontology - look up ancestors for all found terms
			parents = findRelatedNonDefinitiveTerms(iri, TermLinkType.ANCESTORS);
		}

		return parents;
	}

	/**
	 * Look up related terms of a particular type for an IRI.
	 * @param iri the IRI whose related terms are required.
	 * @param type the type of relationship being searched.
	 * @return a collection of IRIs matching the required relationship.
	 */
	private Collection<String> findRelatedNonDefinitiveTerms(String iri, TermLinkType type) {
		Collection<String> terms;

		if (isRelationInCache(iri, type)) {
			terms = retrieveRelatedIrisFromCache(iri, type);
		} else {
			terms = new HashSet<>();
			nonDefinitiveTerms.get(iri).stream().forEach(r -> {
				for (OntologyTerm t : r.getTerms()) {
					try {
						terms.addAll(findRelatedTerms(t, type));
					} catch (OntologyHelperException e) {
						LOGGER.error("Problem getting {} for {} in {}: {}",
								type.toString(), iri, t.getOntologyName(), e.getMessage());
					}
				}
			});
			cacheRelatedIris(iri, type, terms);
		}

		return terms;
	}

	/**
	 * Find all of the terms which are related to a particular ontology term
	 * with a particular relationship.
	 * @param term the term being searched.
	 * @param linkType the type of relationship to look up.
	 * @return a collection of IRIs.
	 * @throws OntologyHelperException if there are problems looking up the
	 * relationship in the web service.
	 */
	private Collection<String> findRelatedTerms(OntologyTerm term, TermLinkType linkType) throws OntologyHelperException {
		Collection<String> iris;

		if (term == null) {
			iris = Collections.emptyList();
		} else if (isRelationInCache(term.getIri(), linkType)) {
			iris = retrieveRelatedIrisFromCache(term.getIri(), linkType);
		} else {
			String linkUrl = getLinkUrl(term, linkType);
			if (linkUrl == null) {
				iris = Collections.emptyList();
			} else {
				iris = queryWebServiceForTerms(linkUrl);
			}
		}

		return iris;
	}

}
