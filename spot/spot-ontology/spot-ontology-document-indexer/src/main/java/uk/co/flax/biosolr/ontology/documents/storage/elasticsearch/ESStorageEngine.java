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

package uk.co.flax.biosolr.ontology.documents.storage.elasticsearch;

import java.util.List;
import java.util.Map;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.api.Document;
import uk.co.flax.biosolr.ontology.documents.storage.StorageEngine;
import uk.co.flax.biosolr.ontology.documents.storage.StorageEngineException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.net.HostAndPort;

/**
 * ElasticSearch implementation of storage engine.
 *
 * @author mlp
 */
public class ESStorageEngine implements StorageEngine {
	
	static final int DEFAULT_PORT = 9300;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ESStorageEngine.class);
	
	private ESConfiguration config;
	
	private Client client;
	
	@Override
	public void setConfiguration(Map<String, Object> configuration) throws StorageEngineException {
		this.config = new ESConfiguration(configuration);
	}

	@SuppressWarnings("resource")
	@Override
	public void initialise() throws StorageEngineException {
		if (config.isUseNodeClient()) {
			Node node = new NodeBuilder().client(true).clusterName(config.getClusterName()).build();
			client = node.client();
		} else {
			TransportAddress[] serverAddresses = config.getServers().stream()
					.map(HostAndPort::fromString)
					.map(hp -> new InetSocketTransportAddress(hp.getHostText(), hp.getPortOrDefault(DEFAULT_PORT)))
					.toArray(size -> new TransportAddress[size]);
			client = new TransportClient().addTransportAddresses(serverAddresses);
		}
	}

	@Override
	public boolean isReady() {
		ClusterHealthResponse response = client.admin().cluster()
			.health(new ClusterHealthRequestBuilder(client.admin().cluster()).request())
			.actionGet();
		return !response.isTimedOut() && (response.getStatus() == ClusterHealthStatus.GREEN || response.getStatus() == ClusterHealthStatus.YELLOW);
	}

	@Override
	public void storeDocuments(List<Document> documents) throws StorageEngineException {
		try {
			ObjectMapper mapper = getSerializationMapper();
			BulkRequestBuilder request = client.prepareBulk();
			
			for (Document doc : documents) {
				IndexRequestBuilder irb = client
						.prepareIndex(config.getIndexName(), config.getIndexType(), doc.getId())
						.setSource(mapper.writeValueAsString(doc));
				
				// Add the index request to the bulk builder
				request.add(irb);
			}
			
			BulkResponse response = request.execute().actionGet();
			if (response.hasFailures()) {
				// Loop through the response and collect the IDs successfully stored
				for (BulkItemResponse itemResponse : response.getItems()) {
					if (itemResponse.isFailed()) {
						LOGGER.error("Could not bulk index item {}: {}", itemResponse.getId(),
								itemResponse.getFailureMessage());
					}
				}
			}
		} catch (JsonProcessingException e) {
			throw new StorageEngineException(e);
		} catch (ElasticsearchException e) {
			throw new StorageEngineException(e);
		}
	}

	/**
	 * Get an object mapper for serializing the product data.
	 * @return the object mapper.
	 */
	private ObjectMapper getSerializationMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		return mapper;
	}

	@Override
	public void flush() throws StorageEngineException {
		// NOP
	}

	@Override
	public void close() throws StorageEngineException {
		try {
			client.close();
		} catch (ElasticsearchException e) {
			LOGGER.error("Problem closing ES client: {}", e.getMessage());
			throw new StorageEngineException(e);
		}
	}

}
