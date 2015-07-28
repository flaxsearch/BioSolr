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

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author mlp
 */
public class OntologyUpdateProcessorFactoryTest extends SolrTestCaseJ4 {

	private static final String ONTOLOGY_UPDATE_CHAIN = "ontology";

	static void checkNumDocs(int n) {
		SolrQueryRequest req = req();
		try {
			assertEquals(n, req.getSearcher().getIndexReader().numDocs());
		} finally {
			req.close();
		}
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		// Initialise a single Solr core
		initCore("solrconfig.xml", "schema.xml", "ontologyUpdate/solr", "documents");
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		clearIndex();
		assertU(commit());
	}

	@Test
	public void test() throws Exception {
		assertNull(h.validateUpdate(adoc("id", "1", "name", "name1")));
		assertNull(h.validateUpdate(commit()));
		checkNumDocs(1);
	}

}
