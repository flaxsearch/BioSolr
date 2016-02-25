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
package uk.co.flax.biosolr.ontology.core.owl;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import uk.co.flax.biosolr.ontology.core.OntologyHelper;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Unit tests for the OWL Ontology Helper methods.
 *
 * <p>Created by Matt Pearce on 20/10/15.</p>
 */
public class OWLOntologyHelperMethodsTest {

	private static OntologyHelper helper;

	public static final String ROOT_IRI = "http://www.w3.org/2002/07/owl#Thing";
	public static final String TEST_IRI = "http://www.ebi.ac.uk/efo/EFO_0000001";
	public static final String TEST_CHILD_IRI = "http://www.ifomis.org/bfo/1.1/snap#MaterialEntity";

	@BeforeClass
	public static void setup() throws URISyntaxException, OWLOntologyCreationException {
		URL testResource = OWLOntologyHelperMethodsTest.class.getClassLoader()
				.getResource(OWLOntologyHelperTest.TEST_ONTOLOGY);
		if (testResource != null) {
			OWLOntologyConfiguration config = new OWLOntologyConfiguration(testResource.toExternalForm(),
					Collections.singletonList(OWLOntologyConfiguration.LABEL_PROPERTY_URI),
					Collections.singletonList(OWLOntologyConfiguration.SYNONYM_PROPERTY_URI),
					Collections.singletonList(OWLOntologyConfiguration.DEFINITION_PROPERTY_URI),
					Collections.emptyList());
			helper = new OWLOntologyHelper(config);
		}
	}

	@AfterClass
	public static void dispose() {
		helper.dispose();
	}

	@Test(expected = NullPointerException.class)
	public void findLabels_nullClass() throws Exception {
		helper.findLabels(null);
	}

	@Test
	public void findLabels() throws Exception {
		Collection<String> labels = helper.findLabels(TEST_IRI);
		assertNotNull(labels);
		assertEquals(2, labels.size());
	}

	@Test(expected = NullPointerException.class)
	public void getChildUris_nullClass() throws Exception {
		helper.getChildIris(null);
	}

	@Test
	public void getChildUris_noChildren() throws Exception {
		Collection<String> childUris = helper.getChildIris("http://www.ebi.ac.uk/efo/PARENTS_001");
		assertNotNull(childUris);
		assertEquals(0, childUris.size());
	}

	@Test
	public void getChildUris() throws Exception {
		Collection<String> childUris = helper.getChildIris(TEST_IRI);
		assertNotNull(childUris);
		assertEquals(2, childUris.size());
		assertTrue(childUris.contains(TEST_CHILD_IRI));
	}

	@Test(expected = NullPointerException.class)
	public void getDescendantUris_nullClass() throws Exception {
		helper.getDescendantIris(null);
	}

	@Test
	public void getDescendantUris_noDescendants() throws Exception {
		Collection<String> descendantUris = helper.getDescendantIris("http://www.ebi.ac.uk/efo/PARENTS_001");
		assertNotNull(descendantUris);
		assertEquals(0, descendantUris.size());
	}

	@Test
	public void getDescendantUris() throws Exception {
		Collection<String> descendantUris = helper.getDescendantIris(TEST_IRI);
		assertNotNull(descendantUris);
		assertEquals(2, descendantUris.size());
		assertTrue(descendantUris.contains(TEST_CHILD_IRI));
	}

	@Test(expected = NullPointerException.class)
	public void getParentUris_nullClass() throws Exception {
		helper.getParentIris(null);
	}

	@Test
	public void getParentUris() throws Exception {
		Collection<String> parentUris = helper.getParentIris(TEST_CHILD_IRI);
		assertNotNull(parentUris);
		assertEquals(1, parentUris.size());
		assertTrue(parentUris.contains(TEST_IRI));
	}

	@Test(expected = NullPointerException.class)
	public void getAncestorUris_nullClass() throws Exception {
		helper.getAncestorIris(null);
	}

	@Test
	public void getAncestorUris() throws Exception {
		Collection<String> ancestorUris = helper.getAncestorIris(TEST_CHILD_IRI);
		assertNotNull(ancestorUris);
		assertEquals(2, ancestorUris.size());
		assertTrue(ancestorUris.contains(TEST_IRI));
		assertTrue(ancestorUris.contains(ROOT_IRI));
	}

	@Test(expected = NullPointerException.class)
	public void findLabelsForIRIs_nullCollection() throws Exception {
		helper.findLabelsForIRIs(null);
	}

	@Test
	public void findLabelsForIRIs_notIRIs() throws Exception {
		final String iri = "blah";
		Collection<String> labels = helper.findLabelsForIRIs(Collections.singletonList(iri));
		assertNotNull(labels);
		assertEquals(0, labels.size());
	}

	@Test
	public void findLabelsForIRIs() throws Exception {
		Collection<String> labels = helper.findLabelsForIRIs(Arrays.asList(TEST_IRI, TEST_CHILD_IRI));
		assertNotNull(labels);
		assertEquals(3, labels.size());
	}

	@Test(expected = NullPointerException.class)
	public void findSynonyms_nullClass() throws Exception {
		helper.findSynonyms(null);
	}

	@Test
	public void findSynonyms_noSynonymsInClass() throws Exception {
		Collection<String> synonyms = helper.findSynonyms(TEST_CHILD_IRI);
		assertNotNull(synonyms);
		assertEquals(0, synonyms.size());
	}

	@Test
	public void findSynonyms() throws Exception {
		Collection<String> synonyms = helper.findSynonyms(TEST_IRI);
		assertNotNull(synonyms);
		assertEquals(1, synonyms.size());
	}

	@Test(expected = NullPointerException.class)
	public void findDefinitions_nullClass() throws Exception {
		helper.findDefinitions(null);
	}

	@Test
	public void findDefinitions_noDefinitionsInClass() throws Exception {
		Collection<String> synonyms = helper.findDefinitions(TEST_CHILD_IRI);
		assertNotNull(synonyms);
		assertEquals(0, synonyms.size());
	}

	@Test
	public void findDefinitions() throws Exception {
		Collection<String> synonyms = helper.findDefinitions(TEST_IRI);
		assertNotNull(synonyms);
		assertEquals(1, synonyms.size());
	}

	@Test
	public void findParentPaths_noParents() throws Exception {
		String iri = TEST_IRI;
		Collection<String> parentPaths = helper.getParentPaths(iri, true);
		assertNotNull(parentPaths);
		assertEquals(1, parentPaths.size());
	}

	@Test
	public void findParentPaths_multiplePaths() throws Exception {
		String iri = "http://www.ebi.ac.uk/efo/PARENTS_001";
		Collection<String> parentPaths = helper.getParentPaths(iri, true);
		assertNotNull(parentPaths);
		assertEquals(2, parentPaths.size());
	}

}
