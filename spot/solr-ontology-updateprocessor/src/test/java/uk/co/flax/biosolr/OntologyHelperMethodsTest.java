/**
 * Copyright (c) 2015 Lemur Consulting Ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.flax.biosolr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 * Unit tests for the OntologyHelper, focussing on methods called
 * after the class has been constructed.
 *
 * @author mlp
 */
public class OntologyHelperMethodsTest {
	
	private static OntologyHelper helper;
	
	public static final String ROOT_IRI = "http://www.w3.org/2002/07/owl#Thing";
	public static final String TEST_IRI = "http://www.ebi.ac.uk/efo/EFO_0000001";
	public static final String TEST_CHILD_IRI = "http://www.ifomis.org/bfo/1.1/snap#MaterialEntity";
	
	@BeforeClass
	public static void setup() throws URISyntaxException, OWLOntologyCreationException {
		URI testOntologyUri = OntologyHelperMethodsTest.class.getClassLoader().getResource(OntologyHelperTest.TEST_ONTOLOGY).toURI();
		helper = new OntologyHelper(testOntologyUri);
	}
	
	@AfterClass
	public static void dispose() {
		helper.dispose();
	}
	
	@Test
	public void getOwlClass_nullValue() {
		OWLClass test = helper.getOwlClass(null);
		assertNull(test);
	}

	@Test
	public void getOwlClass_emptyValue() {
		OWLClass test = helper.getOwlClass("");
		assertNull(test);
	}

	@Test
	public void getOwlClass() {
		OWLClass test = helper.getOwlClass(TEST_IRI);
		assertNotNull(test);
		assertEquals(test.getIRI(), IRI.create(TEST_IRI));
	}

	@Test(expected=java.lang.NullPointerException.class)
	public void findLabels_nullClass() throws Exception {
		helper.findLabels(null);
	}

	@Test
	public void findLabels() throws Exception {
		OWLClass testClass = helper.getOwlClass(TEST_IRI);
		Collection<String> labels = helper.findLabels(testClass);
		assertNotNull(labels);
		assertEquals(1, labels.size());
	}
	
	@Test(expected=java.lang.NullPointerException.class)
	public void getChildUris_nullClass() {
		helper.getChildUris(null);
	}
	
	@Test
	public void getChildUris_noChildren() {
		OWLClass testClass = helper.getOwlClass(TEST_CHILD_IRI);
		Collection<String> childUris = helper.getChildUris(testClass);
		assertNotNull(childUris);
		assertEquals(0, childUris.size());
	}
	
	@Test
	public void getChildUris() {
		OWLClass testClass = helper.getOwlClass(TEST_IRI);
		Collection<String> childUris = helper.getChildUris(testClass);
		assertNotNull(childUris);
		assertEquals(1, childUris.size());
		assertTrue(childUris.contains(TEST_CHILD_IRI));
	}
	
	@Test(expected=java.lang.NullPointerException.class)
	public void getDescendentUris_nullClass() {
		helper.getDescendentUris(null);
	}
	
	@Test
	public void getDescendentUris_noDescendentren() {
		OWLClass testClass = helper.getOwlClass(TEST_CHILD_IRI);
		Collection<String> descendentUris = helper.getDescendentUris(testClass);
		assertNotNull(descendentUris);
		assertEquals(0, descendentUris.size());
	}
	
	@Test
	public void getDescendentUris() {
		OWLClass testClass = helper.getOwlClass(TEST_IRI);
		Collection<String> descendentUris = helper.getDescendentUris(testClass);
		assertNotNull(descendentUris);
		assertEquals(1, descendentUris.size());
		assertTrue(descendentUris.contains(TEST_CHILD_IRI));
	}
	
	@Test(expected=java.lang.NullPointerException.class)
	public void getParentUris_nullClass() {
		helper.getParentUris(null);
	}
	
	@Test
	public void getParentUris() {
		OWLClass testClass = helper.getOwlClass(TEST_CHILD_IRI);
		Collection<String> parentUris = helper.getParentUris(testClass);
		assertNotNull(parentUris);
		assertEquals(1, parentUris.size());
		assertTrue(parentUris.contains(TEST_IRI));
	}
	
	@Test(expected=java.lang.NullPointerException.class)
	public void getAncestorUris_nullClass() {
		helper.getAncestorUris(null);
	}
	
	@Test
	public void getAncestorUris() {
		OWLClass testClass = helper.getOwlClass(TEST_CHILD_IRI);
		Collection<String> ancestorUris = helper.getAncestorUris(testClass);
		assertNotNull(ancestorUris);
		assertEquals(2, ancestorUris.size());
		assertTrue(ancestorUris.contains(TEST_IRI));
		assertTrue(ancestorUris.contains(ROOT_IRI));
	}
	
	@Test(expected=java.lang.NullPointerException.class)
	public void findLabelsForIRIs_nullCollection() {
		helper.findLabelsForIRIs(null);
	}
	
	@Test
	public void findLabelsForIRIs_notIRIs() {
		final String iri = "blah";
		Collection<String> labels = helper.findLabelsForIRIs(Arrays.asList(iri));
		assertNotNull(labels);
		assertEquals(0, labels.size());
	}
	
	@Test
	public void findLabelsForIRIs() {
		Collection<String> labels = helper.findLabelsForIRIs(Arrays.asList(TEST_IRI, TEST_CHILD_IRI));
		assertNotNull(labels);
		assertEquals(2, labels.size());
	}
	
}
