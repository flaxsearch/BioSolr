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
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsBuilder;
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

	private static final String GROUP_FIELD = "study_id";
	private static final String COUNT_AGGREGATION = "numFound";
	private static final String HITS_AGGREGATION = "study";
	private static final String SCORE_AGGREGATION = "topScore";

	private static final String[] DEFAULT_FIELDS = new String[]{
			"title", "first_author", "publication", "efo_uri.label"
	};
	private static final List<String> ANNOTATED_FIELDS = new ArrayList<>();
	static {
		ANNOTATED_FIELDS.add("label");
		ANNOTATED_FIELDS.add("child_labels");
		ANNOTATED_FIELDS.add("parent_labels");
	}

	public ElasticDocumentSearch(Client client, ElasticSearchConfiguration config) {
		super(client, config);
	}

	@Override
	public ResultsList<Document> searchDocuments(String term, int start, int rows,
			List<String> additionalFields, List<String> filters) throws SearchEngineException {
		// Build the query
		MultiMatchQueryBuilder qb = QueryBuilders.multiMatchQuery(term, DEFAULT_FIELDS)
				.minimumShouldMatch("2<25%");
		if (additionalFields != null && additionalFields.size() > 0) {
			List<String> parsedAdditional = parseAdditionalFields(additionalFields);
			parsedAdditional.forEach(qb::field);
		}

		TopHitsBuilder topHitsBuilder = AggregationBuilders.topHits(HITS_AGGREGATION)
				.setFetchSource(true)
				.setFrom(0)
				.setSize(1);
		// Add the annotated fields we need
		ANNOTATED_FIELDS.forEach(fdf -> topHitsBuilder.addFieldDataField(getAnnotationField() + "." + fdf));
		getDynamicFieldNames().forEach(fdf -> topHitsBuilder.addFieldDataField(getAnnotationField() + "." + fdf));

		/* Build the terms aggregation, since we need a result set grouped by study ID.
		 * The "top_score" sub-agg allows us to sort by the top score of the results;
		 * the topHits sub-agg actually pulls back the record data, returning just the first
		 * hit in the aggregation.
		 * Note that we have to get _all_ rows up to and including the last required, annoyingly. */
		AggregationBuilder termsAgg = AggregationBuilders.terms(HITS_AGGREGATION)
				.field(GROUP_FIELD)
				.order(Terms.Order.aggregation(SCORE_AGGREGATION, false))
				.size(start + rows)
				.subAggregation(
						AggregationBuilders.max(SCORE_AGGREGATION)
								.script(new Script("_score", ScriptService.ScriptType.INLINE, "expression", null)))
				.subAggregation(topHitsBuilder);

		// Build the actual search request, including another aggregation to get
		// the number of unique study IDs returned.
		SearchRequestBuilder srb = getClient().prepareSearch(getIndexName())
				.setTypes(getDocumentType())
				.setQuery(qb)
				.setSize(0)
				.addAggregation(termsAgg)
				.addAggregation(AggregationBuilders.cardinality(COUNT_AGGREGATION).field(GROUP_FIELD));
		LOGGER.debug("ES Query: {}", srb.toString());

		SearchResponse response = srb.execute().actionGet();

		// Handle the response
		long total = ((Cardinality)(response.getAggregations().get(COUNT_AGGREGATION))).getValue();
		List<Document> docs;
		if (total == 0) {
			docs = new ArrayList<>();
		} else {
			docs = new ArrayList<>(rows);
			ObjectMapper mapper = buildObjectMapper();

			int lastIdx = (int)(start + rows <= total ? start + rows : total);
			StringTerms terms = response.getAggregations().get(HITS_AGGREGATION);
			List<Terms.Bucket> termBuckets = terms.getBuckets().subList(start, lastIdx);
			for (Terms.Bucket bucket : termBuckets) {
				TopHits hits = bucket.getAggregations().get(HITS_AGGREGATION);
				Document doc = extractDocument(mapper, hits.getHits().getAt(0));
				docs.add(doc);
			}
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
			// Also need to handle hard-coded Solr field names
			parsed = additional.stream()
					.map(add -> add.replaceAll("^efo_uri_(.*)_t$", "$1"))
					.map(add -> getAnnotationField() + "." + add)
					.collect(Collectors.toList());
		}

		return parsed;
	}

	private ObjectMapper buildObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;
	}

	private Document extractDocument(ObjectMapper mapper, SearchHit hit) throws SearchEngineException {
		Document doc;

		try {
			doc = mapper.readValue(hit.getSourceAsString(), Document.class);

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
								List<String> labels = getStringValues(fieldEntry.getValue().getValues());
								if (labels != null) {
									doc.getRelatedLabels().put(shortName, labels);
								}
							}
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error reading document from source: {}", e.getMessage());
			throw new SearchEngineException(e);
		}

		return doc;
	}

	private List<String> getStringValues(List<Object> fieldValues) {
		List<String> retList;
		if (fieldValues == null || fieldValues.size() == 0) {
			retList = null;
		} else {
			retList = fieldValues.stream().map(Object::toString).collect(Collectors.toList());
		}
		return retList;
	}

}
