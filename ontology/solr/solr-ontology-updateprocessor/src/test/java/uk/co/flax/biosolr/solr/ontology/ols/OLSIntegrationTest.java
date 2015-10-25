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
package uk.co.flax.biosolr.solr.ontology.ols;

import org.junit.Test;
import uk.co.flax.biosolr.solr.ontology.OntologyHelper;
import uk.co.flax.biosolr.solr.ontology.owl.OWLOntologyHelperMethodsTest;

import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the OLS Ontology Helper.
 *
 * Created by mlp on 25/10/15.
 * @author mlp
 */
public class OLSIntegrationTest {

	public static final String BASE_URL = "http://www.ebi.ac.uk/ols/beta/api/ontologies";
	public static final String ONTOLOGY = "efo";

	@Test
	public void isIriInOntology() throws Exception {
		OntologyHelper helper = new OLSOntologyHelper(BASE_URL, ONTOLOGY);
		assertTrue(helper.isIriInOntology(OWLOntologyHelperMethodsTest.TEST_IRI));
	}

}
