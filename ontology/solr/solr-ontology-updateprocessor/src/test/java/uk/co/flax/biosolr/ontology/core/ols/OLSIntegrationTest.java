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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.co.flax.biosolr.ontology.core.OntologyHelper;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelperMethodsTest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration tests for the OLS Ontology Helper.
 *
 * Created by mlp on 25/10/15.
 * @author mlp
 */
public class OLSIntegrationTest {

	public static final String BASE_URL = "http://www.ebi.ac.uk/ols/beta/api";
	public static final String ONTOLOGY = "efo";

	public static final String BAD_IRI = "http://blah.com/blah";
	public static final String DESCENDANT_IRI = "http://www.ebi.ac.uk/efo/EFO_0003966";

	public static final String GRAPH_IRI = "http://www.ebi.ac.uk/efo/EFO_0005580";

	private static OntologyHelper helper;

	@BeforeClass
	public static void init() {
		helper = new OLSOntologyHelper(BASE_URL, ONTOLOGY);
	}

	@AfterClass
	public static void shutdown() {
		helper.dispose();
	}

	@Test
	public void isIriInOntology() throws Exception {
		assertTrue(helper.isIriInOntology(OWLOntologyHelperMethodsTest.TEST_IRI));
		assertFalse(helper.isIriInOntology(BAD_IRI));
	}

	@Test
	public void findLabels() throws Exception {
		Collection<String> labels = helper.findLabels(OWLOntologyHelperMethodsTest.TEST_IRI);
		assertNotNull(labels);
		assertTrue(labels.size() == 1);

		labels = helper.findLabels(BAD_IRI);
		assertNotNull(labels);
		assertTrue(labels.isEmpty());
	}

	@Test
	public void findLabelsForIris() throws Exception {
		Collection<String> labels = helper.findLabelsForIRIs(
				Arrays.asList(OWLOntologyHelperMethodsTest.TEST_IRI, OWLOntologyHelperMethodsTest.TEST_CHILD_IRI));
		assertNotNull(labels);
		assertTrue(labels.size() == 2);

		labels = helper.findLabelsForIRIs(
				Arrays.asList(OWLOntologyHelperMethodsTest.TEST_IRI, OWLOntologyHelperMethodsTest.TEST_CHILD_IRI, BAD_IRI));
		assertNotNull(labels);
		assertTrue(labels.size() == 2);
	}

	@Test
	public void findSynonyms() throws Exception {
		Collection<String> synonyms = helper.findSynonyms(OWLOntologyHelperMethodsTest.TEST_IRI);
		assertNotNull(synonyms);
		assertTrue(synonyms.size() == 1);

		synonyms = helper.findSynonyms(BAD_IRI);
		assertNotNull(synonyms);
		assertTrue(synonyms.isEmpty());
	}

	@Test
	public void findDefinitions() throws Exception {
		Collection<String> definitions = helper.findDefinitions(OWLOntologyHelperMethodsTest.TEST_IRI);
		assertNotNull(definitions);
		assertTrue(definitions.size() >= 1);

		definitions = helper.findDefinitions(BAD_IRI);
		assertNotNull(definitions);
		assertTrue(definitions.isEmpty());
	}

	@Test
	public void getChildIris() throws Exception {
		Collection<String> childIris = helper.getChildIris(OWLOntologyHelperMethodsTest.TEST_IRI);
		assertNotNull(childIris);
		assertTrue(childIris.size() >= 1);

		// EFO_0003924 has no children - should not return null!
		childIris = helper.getChildIris("http://www.ebi.ac.uk/efo/EFO_0003924");
		assertNotNull(childIris);
		assertTrue(childIris.isEmpty());

		childIris = helper.getChildIris(BAD_IRI);
		assertNotNull(childIris);
		assertTrue(childIris.isEmpty());
	}

	@Test
	public void getDescendantIris() throws Exception {
		Collection<String> descendantIris = helper.getDescendantIris(DESCENDANT_IRI);
		assertNotNull(descendantIris);
		assertTrue(descendantIris.size() >= 1);

		descendantIris = helper.getDescendantIris(BAD_IRI);
		assertNotNull(descendantIris);
		assertTrue(descendantIris.isEmpty());
	}

	@Test
	public void getParentIris() throws Exception {
		Collection<String> parentIris = helper.getParentIris(OWLOntologyHelperMethodsTest.TEST_CHILD_IRI);
		assertNotNull(parentIris);
		assertTrue(parentIris.size() >= 1);

		parentIris = helper.getParentIris(BAD_IRI);
		assertNotNull(parentIris);
		assertTrue(parentIris.isEmpty());
	}

	@Test
	public void getAncestorIris() throws Exception {
		Collection<String> ancestorIris = helper.getAncestorIris(DESCENDANT_IRI);
		assertNotNull(ancestorIris);
		assertTrue(ancestorIris.size() >= 1);

		ancestorIris = helper.getAncestorIris(BAD_IRI);
		assertNotNull(ancestorIris);
		assertTrue(ancestorIris.isEmpty());
	}

	@Test
	public void getRelations() throws Exception {
		Map<String, Collection<String>> relations = helper.getRelations(GRAPH_IRI);
		assertNotNull(relations);
		assertTrue(relations.size() >= 1);
		assertTrue(relations.containsKey("has_disease_location"));

		relations = helper.getRelations(BAD_IRI);
		assertNotNull(relations);
		assertTrue(relations.isEmpty());
	}

}
