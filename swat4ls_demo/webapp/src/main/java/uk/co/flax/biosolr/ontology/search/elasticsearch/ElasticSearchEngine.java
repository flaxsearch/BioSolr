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
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import uk.co.flax.biosolr.ontology.config.ElasticSearchConfiguration;
import uk.co.flax.biosolr.ontology.search.SearchEngine;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by mlp on 17/02/16.
 *
 * @author mlp
 */
public abstract class ElasticSearchEngine implements SearchEngine {

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
		return null;
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

}
