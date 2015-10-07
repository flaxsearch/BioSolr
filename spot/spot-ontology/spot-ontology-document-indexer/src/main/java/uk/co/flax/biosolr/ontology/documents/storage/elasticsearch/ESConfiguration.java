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

/**
 * ElasticSearch configuration
 *
 * @author mlp
 */
public class ESConfiguration {
	
	static final String DEFAULT_CLUSTER_NAME = "elasticsearch";
	
	static final String NODECLIENT_KEY = "nodeClient";
	static final String SERVERS_KEY = "servers";
	static final String CLUSTERNAME_KEY = "clusterName";
	static final String INDEX_NAME_KEY = "indexName";
	static final String INDEX_TYPE_KEY = "indexType";
	
	private boolean useNodeClient;
	
	private List<String> servers;
	private String clusterName = DEFAULT_CLUSTER_NAME;
	
	private String indexName;
	private String indexType;
	
	@SuppressWarnings("unchecked")
	public ESConfiguration(Map<String, Object> configuration) {
		for (String key : configuration.keySet()) {
			if (key.equals(NODECLIENT_KEY)) {
				useNodeClient = (boolean) configuration.get(key);
			} else if (key.equals(SERVERS_KEY)) {
				servers = (List<String>)configuration.get(key);
			} else if (key.equals(CLUSTERNAME_KEY)) {
				clusterName = (String)configuration.get(key);
			} else if (key.equals(INDEX_NAME_KEY)) {
				indexName = (String)configuration.get(key);
			} else if (key.equals(INDEX_TYPE_KEY)) {
				indexType = (String)configuration.get(key);
			}
		}
	}

	public boolean isUseNodeClient() {
		return useNodeClient;
	}

	public List<String> getServers() {
		return servers;
	}

	public String getClusterName() {
		return clusterName;
	}

	public String getIndexName() {
		return indexName;
	}

	public String getIndexType() {
		return indexType;
	}

}
