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

import java.net.URI;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;

/**
 * Unit tests for the OntologyHelper class.
 *
 * @author mlp
 */
public class OntologyHelperTest {
	
	private static final String TEST_ONTOLOGY = "ontologyUpdate/owl/test.owl";
	private static final String TEST_IRI = "http://www.ebi.ac.uk/efo/EFO_0000001";
	
	private static URI testOntologyUri;
	
	@BeforeClass
	public static void setup() throws Exception {
		testOntologyUri = OntologyHelperTest.class.getClassLoader().getResource(TEST_ONTOLOGY).toURI();
	}
	
	@Test(expected=java.lang.NullPointerException.class)
	public void construct_withNullUri() throws Exception {
		final URI ontologyUri = null;
		new OntologyHelper(ontologyUri);
	}
	
	@Test
	public void construct() throws Exception {
		OntologyHelper helper = new OntologyHelper(testOntologyUri);
		assertNotNull(helper.getOwlClass(TEST_IRI));
	}
	
	@Test(expected=java.lang.NullPointerException.class)
	public void getLabels_nullClass() throws Exception {
		OntologyHelper helper = new OntologyHelper(testOntologyUri);
		helper.findLabels(null);
	}

	@Test
	public void getLabels() throws Exception {
		OntologyHelper helper = new OntologyHelper(testOntologyUri);
		OWLClass testClass = helper.getOwlClass(TEST_IRI);
		Collection<String> labels = helper.findLabels(testClass);
		assertNotNull(labels);
		assertEquals(1, labels.size());
	}

}
