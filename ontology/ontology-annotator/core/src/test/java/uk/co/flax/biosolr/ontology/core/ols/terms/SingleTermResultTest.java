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
import uk.co.flax.biosolr.ontology.core.ols.OLSHttpClientTest;

import static org.junit.Assert.*;

/**
 * Unit tests for the single terms result class.
 *
 * <p>Created by Matt Pearce on 03/12/15.</p>
 * @author Matt Pearce
 */
public class SingleTermResultTest {

	static final String DEFINING_RESULT_FILE = "ols/single_term_result_defining.json";
	static final String NONDEFINING_RESULT_FILE = "ols/single_term_result_nondefining.json";

	@Test
	public void deserialize_fromFile_defining() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		SingleTermResult result = mapper.readValue(
				OLSHttpClientTest.getFile(DEFINING_RESULT_FILE),
				SingleTermResult.class);
		assertNotNull(result);
		assertNotNull(result.getLinks());
		assertNotNull(result.getEmbedded());
		assertFalse(result.getTerms().isEmpty());
		assertTrue(result.hasTerms());
		assertNotNull(result.getPage());
		assertEquals(0, result.getPage().getNumber());
		assertTrue(result.isDefinitiveResult());
		assertNotNull(result.getDefinitiveResult());
	}

	@Test
	public void deserialize_fromFile_nonDefining() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		SingleTermResult result = mapper.readValue(
				OLSHttpClientTest.getFile(NONDEFINING_RESULT_FILE),
				SingleTermResult.class);
		assertNotNull(result);
		assertNotNull(result.getLinks());
		assertNotNull(result.getEmbedded());
		assertFalse(result.getTerms().isEmpty());
		assertTrue(result.hasTerms());
		assertNotNull(result.getPage());
		assertEquals(0, result.getPage().getNumber());
		assertFalse(result.isDefinitiveResult());
		assertNull(result.getDefinitiveResult());
	}

}
