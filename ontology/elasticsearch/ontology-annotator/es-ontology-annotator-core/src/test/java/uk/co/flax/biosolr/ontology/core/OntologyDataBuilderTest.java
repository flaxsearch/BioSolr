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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.co.flax.biosolr.ontology.core.ols.OLSHttpClientTest;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyConfiguration;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelper;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelperMethodsTest;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelperTest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the OntologyDataBuilder.
 * <p>
 * Created by mlp on 21/10/15.
 */
public class OntologyDataBuilderTest {

	public static final String COMPLETE_PROPFILE_PATH = "ontologyUpdate/config/ontology_1.properties";

	private static OntologyHelper helper;

	@BeforeClass
	public static void init() throws Exception {
		helper = new OWLOntologyHelper(OWLOntologyHelperTest.TEST_ONTOLOGY,
				OWLOntologyConfiguration.fromPropertiesFile(OLSHttpClientTest.getFile(COMPLETE_PROPFILE_PATH).getAbsolutePath()));
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
		OntologyData data = new OntologyDataBuilder(helper, iri).includeSynonyms(true).includeDefinitions(true).build();
		assertNotNull(data);
		assertNotNull(data.getLabels());

		// Check synonyms/definitions
		assertNotNull(data.getSynonyms());
		assertNotNull(data.getDefinitions());

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
	public void build_iriNotInOntology() throws Exception {
		final String iri = OWLOntologyHelperMethodsTest.TEST_IRI;

		OntologyHelper helper = mock(OntologyHelper.class);
		when(helper.isIriInOntology(iri)).thenReturn(false);

		OntologyData data = new OntologyDataBuilder(helper, iri).build();
		assertNull(data);

		verify(helper).isIriInOntology(iri);
	}

}
