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

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.ontology.core.ols.terms.OntologyTerm;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelperMethodsTest;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;

/**
 * Unit tests for the OLS HTTP client class.
 *
 * <p>Created by Matt Pearce on 10/12/15.</p>
 * @author Matt Pearce
 */
public class OLSHttpClientTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(OLSHttpClientTest.class);

	private static final int MOCKSERVER_PORT = 1080;
	private static final String MOCKSERVER_DIR = "ols/server";

	public static final String TEST_SERVER = "http://localhost:" + MOCKSERVER_PORT;
	public static final String BASE_PATH = "/ols/beta/api";
	public static final String ONTOLOGY = "efo";

	public static final String BAD_IRI = "http://blah.com/blah";

	private ClientAndServer mockServer;

	@Before
	public void startServer() {
		mockServer = startClientAndServer(MOCKSERVER_PORT);
	}

	@After
	public void stopServer() {
		mockServer.stop();
	}

	@Test
	public void callOLS_withBadIri() throws Exception {
		final String badPath = buildIriPath(BAD_IRI);
		mockServer.when(request().withPath(badPath))
				.respond(response()
						.withStatusCode(HttpStatusCode.NOT_FOUND_404.code())
						.withBody(readBody(MOCKSERVER_DIR + "/err404.json")));

		OLSHttpClient client = new OLSHttpClient(8, null);
		Collection<OntologyTerm> terms = client.callOLS(Collections.singletonList(TEST_SERVER + badPath), OntologyTerm.class);
		client.shutdown();

		assertNotNull(terms);
		assertTrue(terms.isEmpty());
		mockServer.verify(request().withPath(badPath), exactly(1));
	}

	@Test
	public void callOLS_ontologyTerm() throws Exception {
		final String testPath = buildIriPath(OWLOntologyHelperMethodsTest.TEST_IRI);
		mockServer.when(request().withPath(testPath))
				.respond(response()
						.withHeader(new Header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
						.withBody(readBody(MOCKSERVER_DIR + "/efo_0000001.json")));

		OLSHttpClient client = new OLSHttpClient(8, null);
		Collection<OntologyTerm> terms = client.callOLS(Collections.singletonList(TEST_SERVER + testPath), OntologyTerm.class);
		client.shutdown();

		assertNotNull(terms);
		assertFalse(terms.isEmpty());
		mockServer.verify(request().withPath(testPath), exactly(1));
	}

	private static String buildIriPath(String iri) {
		StringBuilder builder = new StringBuilder(BASE_PATH)
				.append(OLSOntologyHelper.ONTOLOGIES_URL_SUFFIX)
				.append("/")
				.append(ONTOLOGY)
				.append(OLSOntologyHelper.TERMS_URL_SUFFIX)
				.append("/");

		try {
			String encIri = URLEncoder.encode(URLEncoder.encode(iri, OLSOntologyHelper.ENCODING), OLSOntologyHelper.ENCODING);
			builder.append(encIri);
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return builder.toString();
	}

	private static String readBody(String file) {
		String body = "";

		try {
			body = FileUtils.readFileToString(getFile(file));
		} catch (IOException | URISyntaxException e) {
			LOGGER.error(e.getMessage());
		}

		return body;
	}

	public static File getFile(String filepath) throws URISyntaxException {
		URL fileUrl = OLSHttpClientTest.class.getClassLoader().getResource(filepath);
		if (fileUrl == null) {
			throw new URISyntaxException(filepath, "Cannot build file URL");
		}
		return new File(fileUrl.toURI());
	}

}
