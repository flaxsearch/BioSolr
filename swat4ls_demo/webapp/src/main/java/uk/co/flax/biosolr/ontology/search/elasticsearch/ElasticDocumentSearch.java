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

import org.elasticsearch.client.Client;
import uk.co.flax.biosolr.ontology.api.Document;
import uk.co.flax.biosolr.ontology.config.ElasticSearchConfiguration;
import uk.co.flax.biosolr.ontology.search.DocumentSearch;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

import java.util.List;

/**
 * Created by mlp on 17/02/16.
 *
 * @author mlp
 */
public class ElasticDocumentSearch extends ElasticSearchEngine implements DocumentSearch {

	public ElasticDocumentSearch(Client client, ElasticSearchConfiguration config) {
		super(client, config);
	}

	@Override
	public ResultsList<Document> searchDocuments(String term, int start, int rows, List<String> additionalFields, List<String> filters) throws SearchEngineException {
		return null;
	}

	@Override
	public ResultsList<Document> searchByEfoUri(int start, int rows, String term, String... uris) throws SearchEngineException {
		return null;
	}
}
