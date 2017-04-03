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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import uk.co.flax.biosolr.ontology.core.ols.OLSHttpClientTest;
import uk.co.flax.biosolr.ontology.core.ols.ObjectMapperResolver;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the OntologyTerm object.
 *
 * <p>Created by Matt Pearce on 21/10/15.</p>
 * @author Matt Pearce
 */
public class OntologyTermTest {

	static final String TERMS_FILE = "ols/ols_terms.json";

	@Test
	public void deserialize_fromFile() throws Exception {
		ObjectMapper mapper = new ObjectMapperResolver().getContext(OntologyTerm.class);

		OntologyTerm terms = mapper.readValue(
				OLSHttpClientTest.getFile(TERMS_FILE),
				OntologyTerm.class);
		assertNotNull(terms);
		assertNotNull(terms.getIri());
		assertNotNull(terms.getDescription());
		assertTrue(StringUtils.isNotBlank(terms.getDescription().get(0)));
		assertNull(terms.getSynonyms());
		assertNotNull(terms.getLinks());
		assertNotNull(terms.getLinks().get(TermLinkType.SELF.toString()));
	}

}
