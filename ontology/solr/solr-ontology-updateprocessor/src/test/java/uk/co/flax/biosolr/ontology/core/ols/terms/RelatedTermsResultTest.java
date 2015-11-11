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
package uk.co.flax.biosolr.ontology.core.ols.terms;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import uk.co.flax.biosolr.solr.ontology.SolrOntologyHelperFactoryTest;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for the RelatedTermsResult class.
 *
 * Created by mlp on 27/10/15.
 * @author mlp
 */
public class RelatedTermsResultTest {

	static final String RESULT_FILE = "ols/related_terms_result.json";

	@Test
	public void deserialize_fromFile() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		RelatedTermsResult result = mapper.readValue(
				new File(SolrOntologyHelperFactoryTest.getFilePath(RESULT_FILE)),
				RelatedTermsResult.class);
		assertNotNull(result);
		assertNotNull(result.getLinks());
		assertNotNull(result.getEmbedded());
		assertFalse(result.getTerms().isEmpty());
		assertNotNull(result.getPage());
		assertEquals(0, result.getPage().getNumber());
	}

}
