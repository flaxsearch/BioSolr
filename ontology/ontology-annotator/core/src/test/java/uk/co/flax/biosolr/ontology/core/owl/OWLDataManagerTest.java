/**
 * Copyright (c) 2016 Lemur Consulting Ltd.
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Unit tests for the OWLDataManager.
 * <p>
 * <p>Created by Matt Pearce on 18/02/16.</p>
 *
 * @author Matt Pearce
 */
public class OWLDataManagerTest {

	private static URI testOntologyUri;

	@BeforeClass
	public static void setup() throws URISyntaxException, OWLOntologyCreationException {
		URL url = OWLDataManagerTest.class.getClassLoader()
				.getResource(OWLOntologyHelperTest.TEST_ONTOLOGY);
		if (url == null) {
			throw new RuntimeException("No URL for " + OWLOntologyHelperTest.TEST_ONTOLOGY);
		} else {
			testOntologyUri = url.toURI();
		}
	}

	@Test(expected = uk.co.flax.biosolr.ontology.core.OntologyHelperException.class)
	public void getOntology_badUri() throws Exception {
		final URI dummyUri = new URI("http://localhost:8080/dummy.owl");
		OWLDataManager dm = new OWLDataManager(dummyUri);
		dm.getOntology();
	}

	@Test
	public void getOntology() throws Exception {
		OWLDataManager dm = new OWLDataManager(testOntologyUri);
		OWLOntology ontology = dm.getOntology();
		assertNotNull(ontology);

		// Dispose and get again...
		dm.dispose();
		ontology = dm.getOntology();
		assertNotNull(ontology);
	}

	@Test(expected = uk.co.flax.biosolr.ontology.core.OntologyHelperException.class)
	public void getReasoner_badUri() throws Exception {
		final URI dummyUri = new URI("http://localhost:8080/dummy.owl");
		OWLDataManager dm = new OWLDataManager(dummyUri);
		dm.getReasoner();
	}

	@Test
	public void getReasoner() throws Exception {
		OWLDataManager dm = new OWLDataManager(testOntologyUri);
		OWLReasoner reasoner = dm.getReasoner();
		assertNotNull(reasoner);

		// Dispose and get again...
		dm.dispose();
		reasoner = dm.getReasoner();
		assertNotNull(reasoner);
	}

	@Test
	public void isIriInOntology_nullIri() throws Exception {
		final IRI iri = null;
		OWLDataManager dm = new OWLDataManager(testOntologyUri);
		assertFalse(dm.isIriInOntology(iri));
	}

	@Test
	public void isIriInOntology_noSuchIri() throws Exception {
		final IRI iri = IRI.create("http://www.ebi.ac.uk/efo/dummy");
		OWLDataManager dm = new OWLDataManager(testOntologyUri);
		assertFalse(dm.isIriInOntology(iri));
	}

	@Test
	public void isIriInOntology() throws Exception {
		final IRI iri = IRI.create(OWLOntologyHelperMethodsTest.TEST_IRI);
		OWLDataManager dm = new OWLDataManager(testOntologyUri);
		assertTrue(dm.isIriInOntology(iri));
	}

	@Test
	public void getOWLClass_nullIri() throws Exception {
		final IRI iri = null;
		OWLDataManager dm = new OWLDataManager(testOntologyUri);
		assertNull(dm.getOWLClass(iri));
	}

	@Test
	public void getOWLClass_noSuchIri() throws Exception {
		final IRI iri = IRI.create("http://www.ebi.ac.uk/efo/dummy");
		OWLDataManager dm = new OWLDataManager(testOntologyUri);
		assertNull(dm.getOWLClass(iri));
	}

	@Test
	public void getOWLClass() throws Exception {
		final IRI iri = IRI.create(OWLOntologyHelperMethodsTest.TEST_IRI);
		OWLDataManager dm = new OWLDataManager(testOntologyUri);
		OWLClass clazz = dm.getOWLClass(iri);
		assertEquals(iri, clazz.getIRI());
	}

}
