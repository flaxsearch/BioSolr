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
package uk.co.flax.biosolr.ontology.core;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyConfiguration;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelper;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelperMethodsTest;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelperTest;

import java.util.Arrays;
import java.util.Collections;

/**
 * Unit tests for the OntologyDataBuilder.
 * <p>
 * <p>Created by Matt Pearce on 21/10/15.</p>
 */
public class OntologyDataBuilderTest {

	private static OntologyHelper helper;

	@BeforeClass
	public static void init() throws Exception {
		OWLOntologyConfiguration config = new OWLOntologyConfiguration(OWLOntologyHelperTest.TEST_ONTOLOGY,
				Collections.singletonList("http://www.w3.org/2000/01/rdf-schema#label"),
				Arrays.asList("http://www.ebi.ac.uk/efo/alternative_term", OWLOntologyConfiguration.SYNONYM_PROPERTY_URI),
				Arrays.asList("http://www.ebi.ac.uk/efo/definition", OWLOntologyConfiguration.DEFINITION_PROPERTY_URI),
				Collections.singletonList("http://www.geneontology.org/formats/oboInOwl#ObsoleteClass"));
		helper = new OWLOntologyHelper(config);
	}

	@AfterClass
	public static void tearDown() {
		helper.dispose();
	}

	@Test
	public void build_withMissingIri() throws Exception {
		final String iri = "http://blah/blah";
		OntologyData data = new OntologyDataBuilder(helper, iri).build();
		assertNull(data);
	}

	@Test
	public void build_simple() throws Exception {
		final String iri = OWLOntologyHelperMethodsTest.TEST_IRI;
		OntologyData data = new OntologyDataBuilder(helper, iri).build();
		assertNotNull(data);
		assertNotNull(data.getLabels());

		// Check synonyms/definitions - should be null
		assertNull(data.getSynonyms());
		assertNull(data.getDefinitions());

		// Check ancestor/descendant values - should be null
		assertNull(data.getAncestorIris());
		assertNull(data.getAncestorLabels());
		assertNull(data.getDescendantIris());
		assertNull(data.getDescendantLabels());

		// Check relations - should be null
		assertNull(data.getRelationIris());
		assertNull(data.getRelationLabels());
	}

	@Test
	public void build_withSynonymsAndDefinitions() throws Exception {
		final String iri = OWLOntologyHelperMethodsTest.TEST_IRI;
		OntologyData data = new OntologyDataBuilder(helper, iri)
				.includeSynonyms(true)
				.includeDefinitions(true)
				.build();
		assertNotNull(data);
		assertNotNull(data.getLabels());

		// Check synonyms/definitions
		assertNotNull(data.getSynonyms());
		assertNotNull(data.getDefinitions());

		// Check that the data reports that it has synonyms/definitions
		assertTrue(data.hasSynonyms());
		assertTrue(data.hasDefinitions());

		// Check ancestor/descendant values - should be null
		assertNull(data.getAncestorIris());
		assertNull(data.getAncestorLabels());
		assertNull(data.getDescendantIris());
		assertNull(data.getDescendantLabels());

		// Check relations - should be null
		assertNull(data.getRelationIris());
		assertNull(data.getRelationLabels());
	}

	@Test
	public void build_withoutIndirectRelations() throws Exception {
		final String iri = "http://www.ifomis.org/bfo/1.1/snap#MaterialEntity";
		OntologyData data = new OntologyDataBuilder(helper, iri)
				.includeIndirect(false)
				.build();
		assertNotNull(data);
		assertNotNull(data.getLabels());

		// Check direct child/parent relations
		assertNotNull(data.getChildIris());
		assertNotNull(data.getChildLabels());
		assertNotNull(data.getParentIris());
		assertNotNull(data.getParentLabels());

		// Check indirect child/parent relations
		assertNull(data.getDescendantIris());
		assertNull(data.getDescendantLabels());
		assertNull(data.getAncestorIris());
		assertNull(data.getAncestorLabels());
	}

	@Test
	public void build_withIndirectRelations() throws Exception {
		final String iri = "http://www.ifomis.org/bfo/1.1/snap#MaterialEntity";
		OntologyData data = new OntologyDataBuilder(helper, iri)
				.includeIndirect(true)
				.build();
		assertNotNull(data);
		assertNotNull(data.getLabels());

		// Check direct child/parent relations
		assertNotNull(data.getChildIris());
		assertNotNull(data.getChildLabels());
		assertNotNull(data.getParentIris());
		assertNotNull(data.getParentLabels());

		// Check indirect child/parent relations
		assertNotNull(data.getDescendantIris());
		assertNotNull(data.getDescendantLabels());
		assertNotNull(data.getAncestorIris());
		assertNotNull(data.getAncestorLabels());
	}

	@Test
	public void build_withRelations() throws Exception {
		final String iri = "http://www.ifomis.org/bfo/1.1/snap#MaterialEntity";
		OntologyData data = new OntologyDataBuilder(helper, iri)
				.includeRelations(true)
				.build();
		assertNotNull(data);
		assertNotNull(data.getLabels());

		// Check synonyms/definitions
		assertNotNull(data.getRelationIris());
		assertTrue(data.getRelationIris().containsKey("participates_in"));
		assertNotNull(data.getRelationLabels());
		assertTrue(data.getRelationLabels().containsKey("participates_in"));
	}

	@Test
	public void build_withParentPaths() throws Exception {
		final String iri = "http://www.ebi.ac.uk/efo/PARENTS_001";
		OntologyData data = new OntologyDataBuilder(helper, iri)
				.includeParentPaths(true)
				.build();
		assertNotNull(data);
		assertNotNull(data.getLabels());

		// Check parent paths
		assertNotNull(data.getParentPaths());
		assertEquals(2, data.getParentPaths().size());
	}

	@Test
	public void build_withParentPathLabels_noParentPaths() throws Exception {
		final String iri = "http://www.ebi.ac.uk/efo/PARENTS_001";
		OntologyData data = new OntologyDataBuilder(helper, iri)
				.includeParentPaths(false)
				.includeParentPathLabels(true)
				.build();
		assertNotNull(data);
		assertNotNull(data.getLabels());

		// Check parent paths - should be null
		assertNull(data.getParentPaths());
	}

	@Test
	public void build_iriNotInOntology() throws Exception {
		final String iri = OWLOntologyHelperMethodsTest.TEST_IRI;

		OntologyHelper helper = mock(OntologyHelper.class);
		when(helper.isIriInOntology(iri)).thenReturn(false);

		OntologyData data = new OntologyDataBuilder(helper, iri).build();
		assertNull(data);

		verify(helper).isIriInOntology(iri);
	}

}
