/**
 * Copyright (c) 2016 Lemur Consulting Ltd.
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
package elasticsearch;

import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.StreamsUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import uk.co.flax.biosolr.elasticsearch.OntologyHelperBuilder;
import uk.co.flax.biosolr.elasticsearch.OntologyUpdatePlugin;

import java.util.Collection;
import java.util.Collections;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertNoFailures;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created by mlp on 12/01/16.
 *
 * @author mlp
 */
public class OLSOntologyUpdateIntegrationTests extends ESIntegTestCase {

	public static final String TEST_IRI = "http://www.ebi.ac.uk/efo/EFO_0000001";
	public static final String TEST_CHILD_IRI = "http://www.ifomis.org/bfo/1.1/snap#MaterialEntity";

	private final static String INDEX_NAME = "test";
	private final static String DOC_TYPE_NAME = "test";
	private static final String ANNOTATION_FIELD = "annotation";

	private final static String MAPPING_FILE = "/mapping/ols-ontology-mapping.json";

	@Override
	protected Collection<Class<? extends Plugin>> nodePlugins() {
		return Collections.singleton(OntologyUpdatePlugin.class);
	}

	@Before
	public void createEmptyIndex() throws Exception {
		logger.info("creating index [{}]", INDEX_NAME);
		createIndex(INDEX_NAME);
		ensureGreen();
	}

	@AfterClass
	public static void shutdownOntologyHelper() {
		OntologyHelperBuilder.getInstance().close();
	}

	@Test
	public void indexAnnotatedDocuments() throws Exception {
		String mapping = StreamsUtils.copyToStringFromClasspath(MAPPING_FILE);
		client().admin().indices().putMapping(new PutMappingRequest(INDEX_NAME).type(DOC_TYPE_NAME).source(mapping))
				.actionGet();

		// Add the root record
		XContentBuilder source = XContentFactory.jsonBuilder().startObject().field(ANNOTATION_FIELD, TEST_IRI).field("name", randomRealisticUnicodeOfLength(12)).endObject();
		IndexResponse response = index(INDEX_NAME, DOC_TYPE_NAME, source);
		String id = response.getId();
		flush();

		QueryBuilder query = QueryBuilders.idsQuery(DOC_TYPE_NAME).addIds(id);
		SearchResponse searchResponse = client().prepareSearch(INDEX_NAME)
				.setTypes(DOC_TYPE_NAME)
				.setFetchSource(true)
				.addFields("annotation.uri", "annotation.label")
				.setQuery(query).get();
		assertNoFailures(searchResponse);
		SearchHits hits = searchResponse.getHits();
		assertThat(hits.getTotalHits(), equalTo(1L));
	}

}
