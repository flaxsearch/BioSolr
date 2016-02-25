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
package uk.co.flax.biosolr.ontology.core;

import static org.junit.Assert.*;

import org.junit.Test;
import uk.co.flax.biosolr.ontology.core.ols.OLSOntologyHelper;
import uk.co.flax.biosolr.ontology.core.ols.OLSTermsOntologyHelper;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelper;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelperTest;

/**
 * Unit tests for the OntologyHelperBuilder.
 *
 * <p>Created by Matt Pearce on 24/02/16.</p>
 * @author Matt Pearce
 */
public class OntologyHelperBuilderTest {

	@Test(expected = uk.co.flax.biosolr.ontology.core.OntologyHelperException.class)
	public void build_noProperties() throws Exception {
		new OntologyHelperBuilder().build();
	}

	@Test
	public void build_owlHelper() throws Exception {
		final String owlOntologyUri = OWLOntologyHelperTest.TEST_ONTOLOGY;
		OntologyHelper helper = new OntologyHelperBuilder().ontologyUri(owlOntologyUri).build();

		assertNotNull(helper);
		assertTrue(helper instanceof OWLOntologyHelper);
	}

	@Test
	public void build_olsHelper() throws Exception {
		final String olsBaseUrl = "http://ols.ebi.ac.uk/beta/api";
		final String ontology = "efo";
		OntologyHelper helper = new OntologyHelperBuilder().olsBaseUrl(olsBaseUrl).ontology(ontology).build();

		assertNotNull(helper);
		assertTrue(helper instanceof OLSOntologyHelper);
		assertFalse(helper instanceof OLSTermsOntologyHelper);
	}

	@Test
	public void build_olsTermsHelper() throws Exception {
		final String olsBaseUrl = "http://ols.ebi.ac.uk/beta/api";
		OntologyHelper helper = new OntologyHelperBuilder().olsBaseUrl(olsBaseUrl).build();

		assertNotNull(helper);
		assertTrue(helper instanceof OLSOntologyHelper);
		assertTrue(helper instanceof OLSTermsOntologyHelper);
	}

}
