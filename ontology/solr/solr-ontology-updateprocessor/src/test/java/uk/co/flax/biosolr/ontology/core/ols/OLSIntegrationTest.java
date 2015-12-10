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
import org.apache.http.HttpHeaders;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.ontology.core.OntologyHelper;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelperMethodsTest;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;

/**
 * Integration tests for the OLS Ontology Helper.
 * <p>
 * Created by mlp on 25/10/15.
 *
 * @author mlp
 */
public class OLSIntegrationTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(OLSIntegrationTest.class);

	private static final int MOCKSERVER_PORT = 1080;
	private static final String MOCKSERVER_DIR = "ols/server";

	public static final String TEST_SERVER = "http://localhost:" + MOCKSERVER_PORT;
	public static final String BASE_PATH = "/ols/beta/api";
	public static final String BASE_URL = TEST_SERVER + BASE_PATH;
	public static final String ONTOLOGY = "efo";

	public static final String BAD_IRI = "http://blah.com/blah";
	public static final String DESCENDANT_IRI = "http://www.ebi.ac.uk/efo/EFO_0003966";

	public static final String GRAPH_IRI = "http://www.ebi.ac.uk/efo/EFO_0005580";

	private ClientAndServer mockServer;

	private OntologyHelper helper;

//	@Before
//	public void startHelper() {
//		helper = new OLSOntologyHelper(BASE_URL, ONTOLOGY);
//	}
//
//	@After
//	public void stopHelper() {
//		helper.dispose();
//	}
//
//	@Before
//	public void startServer() {
//		mockServer = startClientAndServer(MOCKSERVER_PORT);
//	}
//
//	@After
//	public void stopServer() {
//		mockServer.stop();
//	}
//
//	@Test
//	public void isIriInOntology_badIri() throws Exception {
//		final String badPath = buildIriPath(BAD_IRI);
//		mockServer.when(request().withPath(badPath))
//				.respond(response()
//						.withStatusCode(HttpStatusCode.NOT_FOUND_404.code())
//						.withBody(buildIriPath("/err404.json")));
//
//		assertFalse(helper.isIriInOntology(BAD_IRI));
//		mockServer.verify(request().withPath(badPath), exactly(1));
//	}
//
//	@Test
//	public void isIriInOntology() throws Exception {
//		final String testPath = buildIriPath(OWLOntologyHelperMethodsTest.TEST_IRI);
//		mockServer.when(request().withPath(testPath))
//				.respond(response()
//						.withHeader(new Header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
//						.withBody(readBody(MOCKSERVER_DIR + "/efo_0000001.json")));
//
//		assertTrue(helper.isIriInOntology(OWLOntologyHelperMethodsTest.TEST_IRI));
//		mockServer.verify(request().withPath(testPath), exactly(1));
//	}
//
//	@Test
//	public void findLabels_badIri() throws Exception {
//		final String badPath = buildIriPath(BAD_IRI);
//		mockServer.when(request().withPath(badPath))
//				.respond(response()
//						.withStatusCode(HttpStatusCode.NOT_FOUND_404.code())
//						.withBody(buildIriPath("/err404.json")));
//
//		Collection<String> labels = helper.findLabels(BAD_IRI);
//		assertNotNull(labels);
//		assertTrue(labels.isEmpty());
//		mockServer.verify(request().withPath(badPath), exactly(1));
//	}
//
//	@Test
//	public void findLabels() throws Exception {
//		final String testPath = buildIriPath(OWLOntologyHelperMethodsTest.TEST_IRI);
//		mockServer.when(request().withPath(testPath))
//				.respond(response()
//						.withHeader(new Header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
//						.withBody(readBody(MOCKSERVER_DIR + "/efo_0000001.json")));
//
//		Collection<String> labels = helper.findLabels(OWLOntologyHelperMethodsTest.TEST_IRI);
//		assertNotNull(labels);
//		assertTrue(labels.size() == 1);
//		assertEquals("experimental factor", labels.iterator().next());
//		mockServer.verify(request().withPath(testPath), exactly(1));
//	}
//
//	@Test
//	public void findLabelsForIris() throws Exception {
//		final String testPath = buildIriPath(OWLOntologyHelperMethodsTest.TEST_IRI);
//		final String childPath = buildIriPath(OWLOntologyHelperMethodsTest.TEST_CHILD_IRI);
//		mockServer.when(request().withPath(testPath))
//				.respond(response()
//						.withHeader(new Header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
//						.withBody(readBody(MOCKSERVER_DIR + "/efo_0000001.json")));
//		mockServer.when(request().withPath(childPath))
//				.respond(response()
//						.withHeader(new Header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
//						.withBody(readBody(MOCKSERVER_DIR + "/materialEntity.json")));
//		final String badPath = buildIriPath(BAD_IRI);
//		mockServer.when(request().withPath(badPath))
//				.respond(response()
//						.withStatusCode(HttpStatusCode.NOT_FOUND_404.code())
//						.withBody(buildIriPath("/err404.json")));
//
//		Collection<String> labels = helper.findLabelsForIRIs(
//				Arrays.asList(OWLOntologyHelperMethodsTest.TEST_IRI, OWLOntologyHelperMethodsTest.TEST_CHILD_IRI));
//		assertNotNull(labels);
//		assertTrue(labels.size() == 2);
//
//		// Verify server only called once when method called second time
//		labels = helper.findLabelsForIRIs(
//				Arrays.asList(OWLOntologyHelperMethodsTest.TEST_IRI, OWLOntologyHelperMethodsTest.TEST_CHILD_IRI, BAD_IRI));
//		assertNotNull(labels);
//		assertTrue(labels.size() == 2);
//		mockServer.verify(request().withPath(testPath), exactly(1));
//		mockServer.verify(request().withPath(childPath), exactly(1));
//		mockServer.verify(request().withPath(badPath), exactly(1));
//	}
//
//	@Test
//	public void findSynonyms() throws Exception {
//		Collection<String> synonyms = helper.findSynonyms(OWLOntologyHelperMethodsTest.TEST_IRI);
//		assertNotNull(synonyms);
//		assertTrue(synonyms.size() == 1);
//
//		synonyms = helper.findSynonyms(BAD_IRI);
//		assertNotNull(synonyms);
//		assertTrue(synonyms.isEmpty());
//	}
//
//	@Test
//	public void findDefinitions() throws Exception {
//		Collection<String> definitions = helper.findDefinitions(OWLOntologyHelperMethodsTest.TEST_IRI);
//		assertNotNull(definitions);
//		assertTrue(definitions.size() >= 1);
//
//		definitions = helper.findDefinitions(BAD_IRI);
//		assertNotNull(definitions);
//		assertTrue(definitions.isEmpty());
//	}
//
//	@Test
//	public void getChildIris() throws Exception {
//		Collection<String> childIris = helper.getChildIris(OWLOntologyHelperMethodsTest.TEST_IRI);
//		assertNotNull(childIris);
//		assertTrue(childIris.size() >= 1);
//
//		// EFO_0003924 has no children - should not return null!
//		childIris = helper.getChildIris("http://www.ebi.ac.uk/efo/EFO_0003924");
//		assertNotNull(childIris);
//		assertTrue(childIris.isEmpty());
//
//		childIris = helper.getChildIris(BAD_IRI);
//		assertNotNull(childIris);
//		assertTrue(childIris.isEmpty());
//	}
//
//	@Test
//	public void getDescendantIris() throws Exception {
//		Collection<String> descendantIris = helper.getDescendantIris(DESCENDANT_IRI);
//		assertNotNull(descendantIris);
//		assertTrue(descendantIris.size() >= 1);
//
//		descendantIris = helper.getDescendantIris(BAD_IRI);
//		assertNotNull(descendantIris);
//		assertTrue(descendantIris.isEmpty());
//	}
//
//	@Test
//	public void getParentIris() throws Exception {
//		Collection<String> parentIris = helper.getParentIris(OWLOntologyHelperMethodsTest.TEST_CHILD_IRI);
//		assertNotNull(parentIris);
//		assertTrue(parentIris.size() >= 1);
//
//		parentIris = helper.getParentIris(BAD_IRI);
//		assertNotNull(parentIris);
//		assertTrue(parentIris.isEmpty());
//	}
//
//	@Test
//	public void getAncestorIris() throws Exception {
//		Collection<String> ancestorIris = helper.getAncestorIris(DESCENDANT_IRI);
//		assertNotNull(ancestorIris);
//		assertTrue(ancestorIris.size() >= 1);
//
//		ancestorIris = helper.getAncestorIris(BAD_IRI);
//		assertNotNull(ancestorIris);
//		assertTrue(ancestorIris.isEmpty());
//	}
//
//	@Test
//	public void getRelations() throws Exception {
//		Map<String, Collection<String>> relations = helper.getRelations(GRAPH_IRI);
//		assertNotNull(relations);
//		assertTrue(relations.size() >= 1);
//		assertTrue(relations.containsKey("has_disease_location"));
//
//		relations = helper.getRelations(BAD_IRI);
//		assertNotNull(relations);
//		assertTrue(relations.isEmpty());
//	}

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
		URL fileUrl = OLSIntegrationTest.class.getClassLoader().getResource(filepath);
		if (fileUrl == null) {
			throw new URISyntaxException(filepath, "Cannot build file URL");
		}
		return new File(fileUrl.toURI());
	}

}
