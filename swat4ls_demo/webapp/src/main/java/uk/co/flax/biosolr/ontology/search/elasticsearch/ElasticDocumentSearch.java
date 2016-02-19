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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.ontology.api.Document;
import uk.co.flax.biosolr.ontology.config.ElasticSearchConfiguration;
import uk.co.flax.biosolr.ontology.search.DocumentSearch;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by mlp on 17/02/16.
 *
 * @author mlp
 */
public class ElasticDocumentSearch extends ElasticSearchEngine implements DocumentSearch {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticDocumentSearch.class);

	private static final String[] DEFAULT_FIELDS = new String[]{
		"title", "first_author", "publication", "efo_uri.label"
	};

	public ElasticDocumentSearch(Client client, ElasticSearchConfiguration config) {
		super(client, config);
	}

	@Override
	public ResultsList<Document> searchDocuments(String term, int start, int rows,
			List<String> additionalFields, List<String> filters) throws SearchEngineException {
		// Build the query
		MultiMatchQueryBuilder qb = QueryBuilders.multiMatchQuery(term, DEFAULT_FIELDS);
		if (additionalFields != null && additionalFields.size() > 0) {
			List<String> parsedAdditional = parseAdditionalFields(additionalFields);
			parsedAdditional.forEach(qb::field);
		}

		SearchResponse response = getClient().prepareSearch(getIndexName())
				.setTypes(getDocumentType())
				.setQuery(qb)
				.addFields("*", "_source")
				.setFrom(start)
				.setSize(rows)
				.execute().actionGet();

		// Handle the response
		long total = response.getHits().getTotalHits();
		List<Document> docs;
		if (total == 0) {
			docs = new ArrayList<>();
		} else {
			docs = extractDocuments(response.getHits().getHits());
		}

		return new ResultsList<>(docs, start, (start / rows), total);
	}

	@Override
	public ResultsList<Document> searchByEfoUri(int start, int rows, String term, String... uris) throws SearchEngineException {
		return null;
	}

	private List<String> parseAdditionalFields(List<String> additional) {
		List<String> parsed;

		if (additional == null || additional.size() == 0) {
			parsed = null;
		} else {
			// Need to add annotation field name to all additional fields
			parsed = additional.stream()
					.map(add -> getAnnotationField() + "." + add)
					.collect(Collectors.toList());
		}

		return parsed;
	}

	private List<Document> extractDocuments(SearchHit[] hits) throws SearchEngineException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<Document> docs = new ArrayList<>(hits.length);

		try {
			for (SearchHit hit : hits) {
				Document doc = mapper.readValue(hit.getSourceAsString(), Document.class);

				for (Map.Entry<String, SearchHitField> fieldEntry : hit.fields().entrySet()) {
					if (fieldEntry.getKey().startsWith(getAnnotationField())) {
						String fieldName = fieldEntry.getKey();

						switch (fieldName) {
							case "efo_uri.label":
								doc.setEfoLabels(getStringValues(fieldEntry.getValue().getValues()));
								break;
							case "efo_uri.child_labels":
								doc.setChildLabels(getStringValues(fieldEntry.getValue().getValues()));
								break;
							case "efo_uri.parent_labels":
								doc.setParentLabels(getStringValues(fieldEntry.getValue().getValues()));
								break;
							default:
								String shortName = fieldName.substring("efo_uri.".length());
								if (fieldName.endsWith("_rel_uris")) {
									doc.getRelatedIris().put(shortName, getStringValues(fieldEntry.getValue().getValues()));
								} else if (fieldName.endsWith("_rel_labels")) {
									doc.getRelatedLabels().put(shortName, getStringValues(fieldEntry.getValue().getValues()));
								}
						}
					}
				}

				docs.add(doc);
			}
		} catch (IOException e) {
			LOGGER.error("Error reading document from source: {}", e.getMessage());
		}

		return docs;
	}

	private List<String> getStringValues(List<Object> fieldValues) {
		List<String> retList;
		if (fieldValues == null) {
			retList = null;
		} else {
			retList = fieldValues.stream().map(Object::toString).collect(Collectors.toList());
		}
		return retList;
	}

}
