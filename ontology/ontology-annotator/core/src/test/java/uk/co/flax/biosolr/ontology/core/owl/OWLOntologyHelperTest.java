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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import uk.co.flax.biosolr.ontology.core.OntologyHelper;

import java.net.URI;
import java.util.Collections;

/**
 * Unit tests for the OWLOntologyHelper, testing construction of the class.
 *
 * <p>Created by Matt Pearce on 20/10/15.</p>
 */
public class OWLOntologyHelperTest {

	public static final String TEST_ONTOLOGY = "ontologyUpdate/owl/test.owl";
	public static final String TEST_IRI = "http://www.ebi.ac.uk/efo/EFO_0000001";

	@Test(expected = java.net.URISyntaxException.class)
	public void constructString_withBadURI() throws Exception {
		final String dummyUri = "http://blah<.com:8080/dummy.owl";
		OWLOntologyConfiguration config = new OWLOntologyConfiguration(dummyUri,
				Collections.singletonList(OWLOntologyConfiguration.LABEL_PROPERTY_URI),
				Collections.singletonList(OWLOntologyConfiguration.SYNONYM_PROPERTY_URI),
				Collections.singletonList(OWLOntologyConfiguration.DEFINITION_PROPERTY_URI),
				Collections.emptyList());
		new OWLOntologyHelper(config);
	}

	@Test
	public void constructString() throws Exception {
		OWLOntologyConfiguration config = new OWLOntologyConfiguration(TEST_ONTOLOGY,
				Collections.singletonList(OWLOntologyConfiguration.LABEL_PROPERTY_URI),
				Collections.singletonList(OWLOntologyConfiguration.SYNONYM_PROPERTY_URI),
				Collections.singletonList(OWLOntologyConfiguration.DEFINITION_PROPERTY_URI),
				Collections.emptyList());
		OntologyHelper helper = new OWLOntologyHelper(config);
		assertNotNull(helper.findLabels(TEST_IRI));
	}

	@Test(expected = NullPointerException.class)
	public void construct_withNullUri() throws Exception {
		final URI ontologyUri = null;
		new OWLOntologyHelper(ontologyUri, null);
	}

	@Test
	public void construct() throws Exception {
		OWLOntologyConfiguration config = new OWLOntologyConfiguration(TEST_ONTOLOGY,
				Collections.singletonList(OWLOntologyConfiguration.LABEL_PROPERTY_URI),
				Collections.singletonList(OWLOntologyConfiguration.SYNONYM_PROPERTY_URI),
				Collections.singletonList(OWLOntologyConfiguration.DEFINITION_PROPERTY_URI),
				Collections.emptyList());
		OntologyHelper helper = new OWLOntologyHelper(config);
		assertNotNull(helper.findLabels(TEST_IRI));
	}

}
