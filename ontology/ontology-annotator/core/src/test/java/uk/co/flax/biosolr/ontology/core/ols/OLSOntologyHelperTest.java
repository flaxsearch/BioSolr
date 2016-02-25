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

import org.junit.Test;
import uk.co.flax.biosolr.ontology.core.ols.terms.OntologyTerm;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelperMethodsTest.TEST_IRI;

/**
 * Unit tests for the OLS Ontology helper class.
 *
 * <p>Created by Matt Pearce on 10/12/15.</p>
 * @author Matt Pearce
 */
public class OLSOntologyHelperTest {

	public static final String BASE_URL = "http://www.ebi.ac.uk/ols/beta/api/";
	public static final String ONTOLOGY = "efo";

	public static final String BAD_IRI = "http://blah.com/blah";

	@SuppressWarnings("unchecked")
	@Test
	public void isIriInOntology_badIri() throws Exception {
		OLSHttpClient client = mock(OLSHttpClient.class);
		when(client.callOLS(isA(Collection.class), eq(OntologyTerm.class))).thenReturn(Collections.emptyList());

		OLSOntologyConfiguration config = new OLSOntologyConfiguration(BASE_URL, ONTOLOGY, OLSOntologyHelper.PAGE_SIZE);

		OLSOntologyHelper helper = new OLSOntologyHelper(config, client);
		assertFalse(helper.isIriInOntology(BAD_IRI));
		assertFalse(helper.isIriInOntology(BAD_IRI));

		verify(client).callOLS(isA(Collection.class), eq(OntologyTerm.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void isIriInOntology() throws Exception {
		OntologyTerm term = mock(OntologyTerm.class);
		when(term.getIri()).thenReturn(TEST_IRI);
		OLSHttpClient client = mock(OLSHttpClient.class);
		when(client.callOLS(isA(Collection.class), eq(OntologyTerm.class))).thenReturn(Collections.singletonList(term));

		OLSOntologyConfiguration config = new OLSOntologyConfiguration(BASE_URL, ONTOLOGY, OLSOntologyHelper.PAGE_SIZE);
		OLSOntologyHelper helper = new OLSOntologyHelper(config, client);
		assertTrue(helper.isIriInOntology(TEST_IRI));
		assertTrue(helper.isIriInOntology(TEST_IRI));

		verify(client).callOLS(isA(Collection.class), eq(OntologyTerm.class));
	}

}
