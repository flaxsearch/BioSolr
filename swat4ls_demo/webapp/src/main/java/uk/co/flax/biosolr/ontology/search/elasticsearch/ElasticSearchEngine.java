/**
 * Copyright (c) 2016 Lemur Consulting Ltd.
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
package uk.co.flax.biosolr.ontology.search.elasticsearch;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthAction;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsAction;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.unit.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.ontology.config.ElasticSearchConfiguration;
import uk.co.flax.biosolr.ontology.search.SearchEngine;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by mlp on 17/02/16.
 *
 * @author mlp
 */
public abstract class ElasticSearchEngine implements SearchEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchEngine.class);

	private static final String DYNAMIC_LABEL_FIELD_REGEX = "^.*_rel_labels$";

	private final Client client;
	private final ElasticSearchConfiguration configuration;

	protected ElasticSearchEngine(Client client, ElasticSearchConfiguration config) {
		this.client = client;
		this.configuration = config;
	}

	@Override
	public boolean isSearchEngineReady() {
		ClusterHealthStatus status = new ClusterHealthRequestBuilder(client, ClusterHealthAction.INSTANCE)
				.setIndices(configuration.getIndexName())
				.setTimeout(new TimeValue(configuration.getTimeoutMillis(), TimeUnit.MILLISECONDS))
				.request()
				.waitForStatus();
		return status != ClusterHealthStatus.RED;
	}

	@Override
	public List<String> getDynamicFieldNames() throws SearchEngineException {
		List<String> fieldNames = new LinkedList<>();

		try {
			GetMappingsRequest req =
					new GetMappingsRequestBuilder(client, GetMappingsAction.INSTANCE, configuration.getIndexName())
							.setTypes(configuration.getDocType())
							.request();
			GetMappingsResponse response = client.admin().indices().getMappings(req).actionGet();
			MappingMetaData metaData = response.getMappings()
					.get(configuration.getIndexName())
					.get(configuration.getDocType());
			Map<String, Object> sourceMap = metaData.getSourceAsMap();
			Object annotationField = ((Map)sourceMap.get("properties")).get(configuration.getAnnotationField());
			Map<String, Object> annotationProperties = (Map<String, Object>)((Map)annotationField).get("properties");

			for (String field : annotationProperties.keySet()) {
				if (field.matches(DYNAMIC_LABEL_FIELD_REGEX)) {
					fieldNames.add(field);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Caught IOException retrieving field source: {}", e.getMessage());
			throw new SearchEngineException(e);
		}

		return fieldNames;
	}

	protected Client getClient() {
		return client;
	}

	protected String getIndexName() {
		return configuration.getIndexName();
	}

	protected String getDocumentType() {
		return configuration.getDocType();
	}

	protected String getAnnotationField() {
		return configuration.getAnnotationField();
	}

}
