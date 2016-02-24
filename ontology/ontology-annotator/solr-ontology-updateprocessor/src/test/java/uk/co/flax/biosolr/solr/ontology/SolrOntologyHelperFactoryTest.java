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
package uk.co.flax.biosolr.solr.ontology;

import static org.junit.Assert.assertTrue;

import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.junit.Test;
import uk.co.flax.biosolr.ontology.core.OntologyHelper;
import uk.co.flax.biosolr.ontology.core.ols.OLSOntologyHelper;
import uk.co.flax.biosolr.ontology.core.ols.OLSTermsOntologyHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for the SolrOntologyHelperFactory.
 *
 * Created by mlp on 20/10/15.
 * @author mlp
 */
public class SolrOntologyHelperFactoryTest {

	public static final String TEST_ONTOLOGY = "ontologyUpdate/owl/test.owl";
	public static final String COMPLETE_PROPFILE_PATH = "ontologyUpdate/config/ontology_1.properties";

	@Test(expected = org.apache.solr.common.SolrException.class)
	public void construct_noParameters() throws Exception {
		SolrParams params = new MapSolrParams(Collections.emptyMap());
		new SolrOntologyHelperFactory(params);
	}

	@Test
	public void construct_missingOLSOntology() throws Exception {
		Map<String, String> paramMap = new HashMap<>();
		paramMap.put(SolrOntologyHelperFactory.OLS_BASE_URL, "http://www.ebi.ac.uk/ols/beta/api");
		SolrOntologyHelperFactory factory = new SolrOntologyHelperFactory(new MapSolrParams(paramMap));
		OntologyHelper helper = factory.buildOntologyHelper();
		assertTrue(helper instanceof OLSTermsOntologyHelper);
	}

	@Test(expected = org.apache.solr.common.SolrException.class)
	public void construct_missingOLSBaseUrl() throws Exception {
		Map<String, String> paramMap = new HashMap<>();
		paramMap.put(SolrOntologyHelperFactory.OLS_ONTOLOGY_NAME, "efo");
		new SolrOntologyHelperFactory(new MapSolrParams(paramMap));
	}

	@Test
	public void buildOntologyHelper_ols() throws Exception {
		Map<String, String> paramMap = new HashMap<>();
		paramMap.put(SolrOntologyHelperFactory.OLS_BASE_URL, "http://www.ebi.ac.uk/ols/beta/api");
		paramMap.put(SolrOntologyHelperFactory.OLS_ONTOLOGY_NAME, "efo");
		SolrOntologyHelperFactory factory = new SolrOntologyHelperFactory(new MapSolrParams(paramMap));
		OntologyHelper helper = factory.buildOntologyHelper();
		assertTrue(helper instanceof OLSOntologyHelper);
	}

}
