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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.ontology.core.AbstractOntologyHelper;
import uk.co.flax.biosolr.ontology.core.OntologyHelperConfiguration;
import uk.co.flax.biosolr.ontology.core.OntologyHelperException;
import uk.co.flax.biosolr.ontology.core.ols.graph.Edge;
import uk.co.flax.biosolr.ontology.core.ols.graph.Graph;
import uk.co.flax.biosolr.ontology.core.ols.graph.Node;
import uk.co.flax.biosolr.ontology.core.ols.terms.*;

import javax.ws.rs.core.UriBuilder;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OLS-specific implementation of OntologyHelper.
 *
 * <p>Created by Matt Pearce on 21/10/15.</p>
 *
 * @author Matt Pearce
 */
public class OLSOntologyHelper extends AbstractOntologyHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(OLSOntologyHelper.class);

	@SuppressWarnings("unused")
	public static final int THREADPOOL_SIZE = 8;
	public static final int PAGE_SIZE = 100;

	static final String ENCODING = "UTF-8";
	static final String ONTOLOGIES_URL_SUFFIX = "/ontologies";
	static final String TERMS_URL_SUFFIX = "/terms";

	static final String SIZE_PARAM = "size";
	static final String PAGE_PARAM = "page";

	private final OLSOntologyConfiguration configuration;

	private final String baseUrl;

	protected final OLSHttpClient olsClient;

	// Map caching the ontology terms after lookup
	private final Map<String, OntologyTerm> terms = new HashMap<>();

	// Related IRI cache, keyed by IRI then relation type
	private final Map<String, Map<TermLinkType, Collection<String>>> relatedIris = new HashMap<>();

	// Graph cache, keyed by IRI
	private final Map<String, Graph> graphs = new HashMap<>();
	private final Map<String, String> graphLabels = new HashMap<>();

	private long lastCallTime;

	public OLSOntologyHelper(OLSOntologyConfiguration config, OLSHttpClient olsClient) {
		this.configuration = config;
		this.baseUrl = buildBaseUrl(config.getOlsBaseUrl(), config.getOntology());
		this.olsClient = olsClient;
	}

	private String buildBaseUrl(final String baseUrl, final String ontology) {
		String url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		if (StringUtils.isNotBlank(ontology)) {
			url = url + ONTOLOGIES_URL_SUFFIX + "/" + ontology;
		}
		return url;
	}

	@Override
	public void updateLastCallTime() {
		lastCallTime = System.currentTimeMillis();
	}

	@Override
	public long getLastCallTime() {
		return lastCallTime;
	}

	@Override
	public void dispose() {
		LOGGER.info("Disposing of OLS ontology helper for {}", configuration.getOntology());
		olsClient.shutdown();

		// Clear caches
		terms.clear();
		relatedIris.clear();
		graphs.clear();
		graphLabels.clear();
	}

	@Override
	protected OntologyHelperConfiguration getConfiguration() {
		return new OntologyHelperConfiguration();
	}

	/**
	 * Check whether a term is in the terms cache and, if not, attempt to add it.
	 *
	 * @param iri the IRI to look up.
	 * @throws OntologyHelperException if problems occur looking up the IRI.
	 */
	protected void checkTerm(String iri) throws OntologyHelperException {
		checkTerms(Collections.singletonList(iri));
	}

	/**
	 * Check whether a collection of terms are in the terms cache, and if not,
	 * attempt to add them. Terms which cannot be found in OLS are added as a
	 * <code>null</code> entry, to avoid looking them up again.
	 *
	 * @param iris the collection of IRIs to be queried.
	 * @throws OntologyHelperException if the lookup is interrupted.
	 */
	private void checkTerms(final Collection<String> iris) throws OntologyHelperException {
		final List<String> lookups = iris.stream()
				.filter(iri -> !terms.containsKey(iri))
				.collect(Collectors.toList());
		if (!lookups.isEmpty()) {
			List<OntologyTerm> foundTerms = lookupTerms(lookups);

			// Add the found terms to the terms map
			foundTerms.forEach(t -> terms.put(t.getIri(), t));

			// For all not found terms, add null entries to terms map
			lookups.forEach(iri -> terms.putIfAbsent(iri, null));
		}
	}

	/**
	 * Look up a collection of terms in OLS.
	 *
	 * @param iris the terms to be queried.
	 * @return a list of those terms which were found.
	 * @throws OntologyHelperException if the lookup is interrupted.
	 */
	protected List<OntologyTerm> lookupTerms(final List<String> iris) throws OntologyHelperException {
		Collection<String> callUrls = createCallUrls(iris);
		return olsClient.callOLS(callUrls, OntologyTerm.class);
	}

	private Collection<String> createCallUrls(List<String> iris) {
		// Build a list of URLs we need to call
		List<String> urls = new ArrayList<>(iris.size());
		for (final String iri : iris) {
			try {
				final String dblEncodedIri = URLEncoder.encode(URLEncoder.encode(iri, ENCODING), ENCODING);
				urls.add(baseUrl + TERMS_URL_SUFFIX + "/" + dblEncodedIri);
			} catch (UnsupportedEncodingException e) {
				// Not expecting to get here
				LOGGER.error(e.getMessage());
			}
		}
		return urls;
	}

	@Override
	public boolean isIriInOntology(String iri) throws OntologyHelperException {
		checkTerm(iri);
		return terms.containsKey(iri) && terms.get(iri) != null;
	}

	@Override
	public Collection<String> findLabels(String iri) throws OntologyHelperException {
		return findLabelsForIRIs(Collections.singletonList(iri));
	}

	@Override
	public Collection<String> findLabelsForIRIs(Collection<String> iris) throws OntologyHelperException {
		Collection<String> labels;

		if (iris == null) {
			labels = Collections.emptyList();
		} else {
			// Check if we have labels in the graph cache
			labels = iris.stream()
					.filter(graphLabels::containsKey)
					.map(graphLabels::get)
					.collect(Collectors.toList());

			if (labels.size() != iris.size()) {
				// Not everything in graph cache - do further lookups
				Collection<String> lookups = iris.stream()
						.filter(i -> !graphLabels.containsKey(i))
						.collect(Collectors.toList());
				checkTerms(lookups);

				labels.addAll(lookups.stream()
						.filter(i -> Objects.nonNull(terms.get(i)))
						.map(i -> terms.get(i).getLabel())
						.collect(Collectors.toList()));
			}
		}

		return labels;
	}

	@Override
	public Collection<String> findSynonyms(String iri) throws OntologyHelperException {
		checkTerm(iri);
		Collection<String> synonyms;
		if (terms.get(iri) != null) {
			synonyms = terms.get(iri).getSynonyms();
		} else {
			synonyms = Collections.emptyList();
		}
		return synonyms;
	}

	@Override
	public Collection<String> findDefinitions(String iri) throws OntologyHelperException {
		checkTerm(iri);
		Collection<String> definitions;
		if (terms.get(iri) != null) {
			definitions = terms.get(iri).getDescription();
		} else {
			definitions = Collections.emptyList();
		}
		return definitions;
	}

	@Override
	public Collection<String> getChildIris(String iri) throws OntologyHelperException {
		checkTerm(iri);
		return findRelatedTerms(terms.get(iri), TermLinkType.CHILDREN);
	}

	@Override
	public Collection<String> getDescendantIris(String iri) throws OntologyHelperException {
		checkTerm(iri);
		return findRelatedTerms(terms.get(iri), TermLinkType.DESCENDANTS);
	}

	@Override
	public Collection<String> getParentIris(String iri) throws OntologyHelperException {
		checkTerm(iri);
		return findRelatedTerms(terms.get(iri), TermLinkType.PARENTS);
	}

	@Override
	public Collection<String> getAncestorIris(String iri) throws OntologyHelperException {
		checkTerm(iri);
		return findRelatedTerms(terms.get(iri), TermLinkType.ANCESTORS);
	}

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
			cacheRelatedIris(term.getIri(), linkType, iris);
		}

		return iris;
	}

	/**
	 * Extract the URL for a particular type of link from an OntologyTerm.
	 *
	 * @param term the term.
	 * @param linkType the type of link required.
	 * @return the URL, or <code>null</code> if the term is null, or doesn't
	 * have a link of the required type.
	 */
	static String getLinkUrl(OntologyTerm term, TermLinkType linkType) {
		String ret = null;

		if (term != null) {
			Link link = term.getLinks().get(linkType.toString());
			if (link != null && StringUtils.isNotBlank(link.getHref())) {
				ret = link.getHref();
			}
		}

		return ret;
	}

	protected boolean isRelationInCache(String iri, TermLinkType relation) {
		return relatedIris.containsKey(iri) && relatedIris.get(iri).containsKey(relation);
	}

	protected Collection<String> retrieveRelatedIrisFromCache(String iri, TermLinkType relation) {
		Collection<String> ret = null;

		if (relatedIris.containsKey(iri) && relatedIris.get(iri).containsKey(relation)) {
			ret = relatedIris.get(iri).get(relation);
		}

		return ret;
	}

	protected void cacheRelatedIris(String iri, TermLinkType relation, Collection<String> iris) {
		if (!relatedIris.containsKey(iri)) {
			relatedIris.put(iri, new HashMap<>());
		}

		if (relatedIris.get(iri).containsKey(relation)) {
			relatedIris.get(iri).get(relation).addAll(iris);
		} else {
			relatedIris.get(iri).put(relation, iris);
		}
	}

	/**
	 * Find the IRIs of all terms referenced by a related URL.
	 *
	 * @param baseUrl the base URL to look up, from a Link or similar
	 * query-type URL.
	 * @return a set of IRIs referencing the terms found for the
	 * given URL.
	 * @throws OntologyHelperException if problems occur accessing the
	 * web service.
	 */
	protected Set<String> queryWebServiceForTerms(String baseUrl) throws OntologyHelperException {
		Set<String> retList;

		// Build URL for first page
		List<String> urls = buildPageUrls(baseUrl, 0, 1);
		// Sort returned calls by page number
		SortedSet<RelatedTermsResult> results = new TreeSet<>(
				(RelatedTermsResult r1, RelatedTermsResult r2) -> r1.getPage().compareTo(r2.getPage()));
		results.addAll(olsClient.callOLS(urls, RelatedTermsResult.class));

		if (results.size() == 0) {
			retList = Collections.emptySet();
		} else {
			Page page = results.first().getPage();
			if (page.getTotalPages() > 1) {
				// Get remaining pages
				urls = buildPageUrls(baseUrl, page.getNumber() + 1, page.getTotalPages());
				results.addAll(olsClient.callOLS(urls, RelatedTermsResult.class));
			}

			retList = new HashSet<>(page.getTotalSize());
			for (RelatedTermsResult result : results) {
				result.getTerms().forEach(t -> {
					terms.put(t.getIri(), t);
					retList.add(t.getIri());
				});
			}
		}

		return retList;
	}

	/**
	 * Build a list of URLs for a range of pages.
	 *
	 * @param baseUrl the base URL; the page size and page number will be appended to
	 * this as query parameters.
	 * @param firstPage the first page in the range, inclusive.
	 * @param lastPage the last page in the range, exclusive.
	 * @return the list of generated URLs.
	 */
	protected List<String> buildPageUrls(String baseUrl, int firstPage, int lastPage) {
		UriBuilder builder = UriBuilder.fromUri(baseUrl)
				.queryParam(SIZE_PARAM, configuration.getPageSize())
				.queryParam(PAGE_PARAM, "{pageNum}");

		List<String> pageUrls = new ArrayList<>(lastPage - firstPage);

		for (int i = firstPage; i < lastPage; i++) {
			pageUrls.add(builder.build(i).toString());
		}

		return pageUrls;
	}

	@Override
	public Map<String, Collection<String>> getRelations(String iri) throws OntologyHelperException {
		Map<String, Collection<String>> relations = new HashMap<>();

		checkTerm(iri);
		Graph graph = lookupGraph(iri);
		if (graph != null) {
			for (Edge e : graph.getEdgesBySource(iri, false)) {
				if (!relations.containsKey(e.getLabel())) {
					relations.put(e.getLabel(), new ArrayList<>());
				}
				relations.get(e.getLabel()).add(e.getTarget());
			}
		}

		return relations;
	}

	private Graph lookupGraph(String iri) throws OntologyHelperException {
		if (!graphs.containsKey(iri)) {
			String graphUrl = getLinkUrl(terms.get(iri), TermLinkType.GRAPH);
			if (graphUrl != null) {
				List<Graph> graphResults = olsClient.callOLS(Collections.singletonList(graphUrl), Graph.class);
				if (graphResults.size() > 0) {
					graphs.put(iri, graphResults.get(0));
					cacheGraphLabels(graphResults.get(0));
				} else {
					graphs.put(iri, null);
				}
			}
		}

		return graphs.get(iri);
	}

	private void cacheGraphLabels(Graph graph) {
		if (graph.getNodes() != null) {
			Map<String, String> nodeLabels = graph.getNodes().stream()
					.filter(n -> !graphLabels.containsKey(n.getIri()))
					.collect(Collectors.toMap(Node::getIri, Node::getLabel));
			graphLabels.putAll(nodeLabels);
		}
	}

	String getBaseUrl() {
		return baseUrl;
	}

}
