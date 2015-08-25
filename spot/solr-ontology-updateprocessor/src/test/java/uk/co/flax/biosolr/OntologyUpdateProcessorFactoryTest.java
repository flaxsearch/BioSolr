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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.common.params.UpdateParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.handler.UpdateRequestHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for the OntologyUpdateProcessorFactory, using SolrTestCaseJ4
 * to add and check some example records.
 * 
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
	public void addDoc_checkLabel() throws Exception {
		addDoc(adoc("id", "1", "name", "name1", "annotation_uri", OntologyHelperTest.TEST_IRI), 
				ONTOLOGY_UPDATE_CHAIN);
		assertU(commit());
		checkNumDocs(1);

		SolrQueryRequest req = req("id:1");
		assertQ("Could not find label", req, "//result[@numFound=1]",
				"//str[@name='ontology_label'][.='experimental factor']");
	}

	@Test
	public void addDoc_checkChildren() throws Exception {
		addDoc(adoc("id", "1", "name", "name1", "annotation_uri", OntologyHelperTest.TEST_IRI), 
				ONTOLOGY_UPDATE_CHAIN);
		assertU(commit());
		checkNumDocs(1);

		SolrQueryRequest req = req("id:1");
		assertQ("Could not find child", req, "//result[@numFound=1]",
				"//arr[@name='child_uris_s']/str[1][.='" + OntologyHelperMethodsTest.TEST_CHILD_IRI + "']",
				"//arr[@name='child_labels_t']/str[1][.='material entity']");
	}

	@Test
	public void addDoc_checkParents() throws Exception {
		addDoc(adoc("id", "1", "name", "name1", "annotation_uri", OntologyHelperMethodsTest.TEST_CHILD_IRI), 
				ONTOLOGY_UPDATE_CHAIN);
		assertU(commit());
		checkNumDocs(1);

		SolrQueryRequest req = req("id:1");
		assertQ("Could not find parent", req, "//result[@numFound=1]",
				"//arr[@name='parent_uris_s']/str[1][.='" + OntologyHelperMethodsTest.TEST_IRI + "']",
				"//arr[@name='parent_labels_t']/str[1][.='experimental factor']");
	}

	@Test
	public void addDoc_checkSynonyms() throws Exception {
		addDoc(adoc("id", "1", "name", "name1", "annotation_uri", OntologyHelperMethodsTest.TEST_IRI), 
				ONTOLOGY_UPDATE_CHAIN);
		assertU(commit());
		checkNumDocs(1);

		SolrQueryRequest req = req("id:1");
		assertQ("Could not find synonyms", req, "//result[@numFound=1]",
				"//arr[@name='synonyms_t']/str[1][.='ExperimentalFactor']");
	}

	@Test
	public void addDoc_checkDefinition() throws Exception {
		addDoc(adoc("id", "1", "name", "name1", "annotation_uri", OntologyHelperMethodsTest.TEST_IRI), 
				ONTOLOGY_UPDATE_CHAIN);
		assertU(commit());
		checkNumDocs(1);

		SolrQueryRequest req = req("id:1");
		assertQ("Could not find definition", req, "//result[@numFound=1]",
				"//arr[@name='definition_t']/str[1][.='An experimental factor in Array Express which are essentially the variable aspects of an experiment design which can be used to describe an experiment, or set of experiments, in an increasingly detailed manner. This upper level class is really used to give a root class from which applications can rely on and not be tied to upper ontology classses which do change.']");
	}

	static void addDoc(String doc, String chain) throws Exception {
		Map<String, String[]> params = new HashMap<>();
		MultiMapSolrParams mmparams = new MultiMapSolrParams(params);
		params.put(UpdateParams.UPDATE_CHAIN, new String[] { chain });
		SolrQueryRequestBase req = new SolrQueryRequestBase(h.getCore(), mmparams) {
		};

		UpdateRequestHandler handler = new UpdateRequestHandler();
		handler.init(null);
		ArrayList<ContentStream> streams = new ArrayList<>(2);
		streams.add(new ContentStreamBase.StringStream(doc));
		req.setContentStreams(streams);
		handler.handleRequestBody(req, new SolrQueryResponse());
		req.close();
	}
}
