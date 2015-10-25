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
package uk.co.flax.biosolr.solr.ontology.ols;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.solr.ontology.OntologyHelper;
import uk.co.flax.biosolr.solr.ontology.OntologyHelperException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * OLS-specific implementation of OntologyHelper.
 *
 * Created by mlp on 21/10/15.
 * @author mlp
 */
public class OLSOntologyHelper implements OntologyHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(OLSOntologyHelper.class);

	private static final String ENCODING = "UTF-8";
	private static final String TERMS_URL_SUFFIX = "/terms";

	private final String baseUrl;
	private final String ontology;

	private final Client client;
	private final ExecutorService executor;

	private final Map<String, OntologyTerms> terms = new HashMap<>();

	private long lastCallTime;

	public OLSOntologyHelper(String baseUrl, String ontology) {
		this.baseUrl = baseUrl;
		this.ontology = ontology;

		this.client = ClientBuilder.newBuilder()
				.register(ObjectMapperResolver.class)
				.register(JacksonFeature.class)
				.build();
		this.executor = Executors.newCachedThreadPool();
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
		executor.shutdown();
		client.close();
	}

	private void checkTerms(final Collection<String> iris) throws OntologyHelperException {
		final List<String> lookups = iris.stream()
				.filter(iri -> !terms.containsKey(iri))
				.collect(Collectors.toList());
		if (!lookups.isEmpty()) {
			List<OntologyTerms> foundTerms = lookupTerms(lookups);

			// Add the found terms to the terms map
			foundTerms.forEach(t -> terms.put(t.getIri(), t));

			// For all not found terms, add null entries to terms map
			lookups.forEach(iri -> terms.putIfAbsent(iri, null));
		}
	}

	private List<OntologyTerms> lookupTerms(final List<String> iris) throws OntologyHelperException {
		List<OntologyTerms> retTerms = new ArrayList<>(iris.size());

		try {
			List<Callable<OntologyTerms>> termsCalls = createTermsCalls(iris);
			List<Future<OntologyTerms>> holders = executor.invokeAll(termsCalls);

			holders.forEach(h -> {
				try {
					retTerms.add(h.get());
				} catch (ExecutionException e) {
					LOGGER.error(e.getMessage(), e);
				} catch (InterruptedException e) {
					LOGGER.error(e.getMessage());
				}
			});
		} catch (InterruptedException e) {
			Thread.interrupted();
			throw new OntologyHelperException(e);
		}

		return retTerms;
	}

	/**
	 * Build a list of callable requests to look up IRI terms.
	 * @param iris the IRIs to look up.
	 * @return a list of Callable requests.
	 */
	private List<Callable<OntologyTerms>> createTermsCalls(List<String> iris) {
		// Build a list of URLs we need to call
		List<String> urls = new ArrayList<>(iris.size());
		for (final String iri : iris) {
			try {
				final String encodedIri = URLEncoder.encode(iri, ENCODING);
				final String dblEncodedIri = URLEncoder.encode(encodedIri, ENCODING);
				urls.add(baseUrl + "/" + ontology + TERMS_URL_SUFFIX + "/" + dblEncodedIri);
			} catch (UnsupportedEncodingException e) {
				// Not expecting to get here
				LOGGER.error(e.getMessage());
			}
		}
		return createCalls(urls, OntologyTerms.class);
	}

	/**
	 * Build a list of calls, each returning the same object type.
	 * @param urls the URLs to be called.
	 * @param clazz the type of object returned by the call.
	 * @param <T> placeholder for the clazz parameter.
	 * @return a list of Callable requests.
	 */
	private <T> List<Callable<T>> createCalls(List<String> urls, Class<T> clazz) {
		List<Callable<T>> calls = new ArrayList<>(urls.size());

		urls.forEach(url -> calls.add(() ->
			client.target(url).request(MediaType.APPLICATION_JSON_TYPE).get(clazz)
		));

		return calls;
	}

	@Override
	public boolean isIriInOntology(String iri) throws OntologyHelperException {
		checkTerms(Collections.singletonList(iri));
		return terms.containsKey(iri) && terms.get(iri) != null;
	}

	@Override
	public Collection<String> findLabels(String iri) {
		return null;
	}

	@Override
	public Collection<String> findLabelsForIRIs(Collection<String> iris) {
		return null;
	}

	@Override
	public Collection<String> findSynonyms(String iri) {
		return null;
	}

	@Override
	public Collection<String> findDefinitions(String iri) {
		return null;
	}

	@Override
	public Collection<String> getChildIris(String iri) {
		return null;
	}

	@Override
	public Collection<String> getDescendantIris(String iri) {
		return null;
	}

	@Override
	public Collection<String> getParentIris(String iri) {
		return null;
	}

	@Override
	public Collection<String> getAncestorIris(String iri) {
		return null;
	}

	@Override
	public Map<String, Collection<String>> getRelations(String iri) {
		return null;
	}

}
