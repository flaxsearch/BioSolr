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
package uk.co.flax.biosolr.solr.update.processor;

import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * OLS-specific version of the OntologyUpdateProcessorFactoryTest.
 *
 * Created by mlp on 10/11/15.
 * @author mlp
 */
public class OLSOntologyUpdateProcessorFactoryTest extends OntologyUpdateProcessorFactoryTest {

	@BeforeClass
	public static void beforeClass() throws Exception {
		// Initialise a single Solr core
		initCore("solrconfig.xml", "schema.xml", "ontologyUpdate/solr", "olsdocuments");
	}

	@Test
	public void addDoc_checkChildren() throws Exception {
		addDoc(adoc("id", "1", "name", "name1", "annotation_uri", TEST_IRI),
				ONTOLOGY_UPDATE_CHAIN);
		assertU(commit());
		checkNumDocs(1);

		SolrQueryRequest req = req("id:1");
		assertQ("Could not find child", req, "//result[@numFound=1]",
				"//arr[@name='annotation_uri_child_uris_s']/str[1][.='http://purl.obolibrary.org/obo/IAO_0000030']",
				"//arr[@name='annotation_uri_child_labels_t']/str[1][.='information entity']");
	}

	@Test
	public void addDoc_checkParents() throws Exception {
		addDoc(adoc("id", "1", "name", "name1", "annotation_uri", "http://purl.obolibrary.org/obo/IAO_0000030"),
				ONTOLOGY_UPDATE_CHAIN);
		assertU(commit());
		checkNumDocs(1);

		SolrQueryRequest req = req("id:1");
		assertQ("Could not find parent", req, "//result[@numFound=1]",
				"//arr[@name='annotation_uri_parent_uris_s']/str[1][.='" + TEST_IRI + "']",
				"//arr[@name='annotation_uri_parent_labels_t']/str[1][.='experimental factor']");
	}
}
