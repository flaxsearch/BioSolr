package elasticsearch; /**
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

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsAction;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
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
import uk.co.flax.biosolr.elasticsearch.mapper.ontology.FieldMappings;

import java.util.Collection;
import java.util.Collections;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertNoFailures;
import static org.hamcrest.Matchers.equalTo;

/**
 * Integration tests for the OntologyMapper.
 *
 * @author mlp
 */
public class OntologyUpdateIntegrationTests extends ESIntegTestCase {

	public static final String TEST_IRI = "http://www.ebi.ac.uk/efo/EFO_0000001";
	public static final String TEST_CHILD_IRI = "http://www.ifomis.org/bfo/1.1/snap#MaterialEntity";

	private final static String INDEX_NAME = "test";
	private final static String DOC_TYPE_NAME = "test";
	private static final String ANNOTATION_FIELD = "annotation";

	private final static String MAPPING_FILE = "/mapping/ontology-mapping.json";

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
		assertAcked(client().admin().indices().putMapping(
				new PutMappingRequest(INDEX_NAME).type(DOC_TYPE_NAME).source(mapping))
				.actionGet());

		// Add the root record
		XContentBuilder source = XContentFactory.jsonBuilder().startObject()
				.field(ANNOTATION_FIELD, TEST_IRI)
				.field("name", randomRealisticUnicodeOfLength(12))
				.endObject();
		IndexResponse response = index(INDEX_NAME, DOC_TYPE_NAME, source);
		String id = response.getId();
		flush();

		GetMappingsResponse mappingsResponse = client().admin().indices().getMappings(
				new GetMappingsRequestBuilder(client(), GetMappingsAction.INSTANCE, INDEX_NAME).setTypes(DOC_TYPE_NAME).request())
				.actionGet();

		QueryBuilder query = QueryBuilders.idsQuery(DOC_TYPE_NAME).addIds(id);
		SearchResponse searchResponse = client().prepareSearch(INDEX_NAME).setTypes(DOC_TYPE_NAME)
				.setFetchSource(true)
				.addFields("annotation.uri", "annotation.label", "annotation.*")
				.setQuery(query)
				.get();
		assertNoFailures(searchResponse);
		SearchHits hits = searchResponse.getHits();
		assertThat(hits.getTotalHits(), equalTo(1L));

		query = QueryBuilders.termQuery(ANNOTATION_FIELD + "." + FieldMappings.URI.getFieldName(), TEST_IRI);
		searchResponse = client().prepareSearch(INDEX_NAME).setTypes(DOC_TYPE_NAME)
				.setFetchSource(true)
				.addFields("annotation.uri", "annotation.label",
						"annotation.child_uris", "annotation.descendant_uris",
						"annotation.parent_uris", "annotation.ancestor_uris")
				.setQuery(query).get();
		assertNoFailures(searchResponse);
		hits = searchResponse.getHits();
		assertThat(hits.getTotalHits(), equalTo(1L));
		assertThat(hits.getHits()[0].field("annotation.child_uris").getValues().get(0), equalTo(TEST_CHILD_IRI));
		assertThat(hits.getHits()[0].field("annotation.descendant_uris").getValues().get(0), equalTo(TEST_CHILD_IRI));
		assertThat(hits.getHits()[0].field("annotation.parent_uris").getValues().size(), equalTo(1));
		assertThat(hits.getHits()[0].field("annotation.ancestor_uris").getValues().size(), equalTo(1));

		query = QueryBuilders.matchQuery(ANNOTATION_FIELD + "." + FieldMappings.LABEL.getFieldName(), "experimental");
		searchResponse = client().prepareSearch(INDEX_NAME).setTypes(DOC_TYPE_NAME)
				.setFetchSource(true)
				.addFields("annotation.uri", "annotation.label")
				.setQuery(query).get();
		assertNoFailures(searchResponse);
		hits = searchResponse.getHits();
		assertThat(hits.getTotalHits(), equalTo(1L));
		assertThat(hits.getHits()[0].field(ANNOTATION_FIELD + "." + FieldMappings.LABEL.getFieldName()).getValues().size(), equalTo(2));

		// Add the child record
		source = XContentFactory.jsonBuilder().startObject()
				.field(ANNOTATION_FIELD, TEST_CHILD_IRI)
				.field("name", randomRealisticUnicodeOfLength(12))
				.endObject();
		response = index(INDEX_NAME, DOC_TYPE_NAME, source);
		flush();

		mappingsResponse = client().admin().indices().getMappings(new GetMappingsRequest()).actionGet();

		query = QueryBuilders.termQuery(ANNOTATION_FIELD + "." + FieldMappings.URI.getFieldName(), TEST_CHILD_IRI);
		searchResponse = client().prepareSearch(INDEX_NAME).setTypes(DOC_TYPE_NAME)
				.setFetchSource(true)
				.addFields("name", "annotation", "*")
				.setQuery(query)
				.get();
		assertNoFailures(searchResponse);
		hits = searchResponse.getHits();
		assertThat(hits.getTotalHits(), equalTo(1L));
		assertNotNull(hits.getHits()[0].field("annotation.participates_in_rel_uris"));
		assertThat(hits.getHits()[0].field("annotation.participates_in_rel_uris").getValues().get(0), equalTo(TEST_IRI));
		assertNotNull(hits.getHits()[0].field("annotation.participates_in_rel_labels").getValues().get(0));
	}

}
