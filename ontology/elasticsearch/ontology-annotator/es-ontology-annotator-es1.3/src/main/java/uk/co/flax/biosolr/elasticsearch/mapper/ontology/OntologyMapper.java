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

package uk.co.flax.biosolr.elasticsearch.mapper.ontology;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.hppc.ObjectObjectOpenHashMap;
import org.elasticsearch.common.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.netty.util.internal.ConcurrentHashMap;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.*;
import org.elasticsearch.threadpool.ThreadPool;
import uk.co.flax.biosolr.ontology.core.OntologyData;
import uk.co.flax.biosolr.ontology.core.OntologyDataBuilder;
import uk.co.flax.biosolr.ontology.core.OntologyHelper;
import uk.co.flax.biosolr.ontology.core.OntologyHelperException;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Mapper class to expand ontology details from an ontology
 * annotation field value.
 *
 * @author mlp
 */
public class OntologyMapper implements Mapper {

	public static final long DELETE_CHECK_DELAY_MS = 15 * 60 * 1000; // 15 minutes

	public static final String DYNAMIC_URI_FIELD_SUFFIX = "_rel_uris";
	public static final String DYNAMIC_LABEL_FIELD_SUFFIX = "_rel_labels";

	private static final ESLogger logger = ESLoggerFactory.getLogger(OntologyMapper.class.getName());


	public static class Builder extends Mapper.Builder<Builder, OntologyMapper> {

		private final OntologySettings ontologySettings;
		private final ThreadPool threadPool;

		public Builder(String name, OntologySettings ontSettings, ThreadPool threadPool) {
			super(name);
			this.ontologySettings = ontSettings;
			this.threadPool = threadPool;
		}

		@Override
		public OntologyMapper build(BuilderContext context) {
			Map<FieldMappings, FieldMapper<String>> fieldMappers = Maps.newHashMap();

			context.path().add(name);

			for (FieldMappings mapping : FieldMappings.values()) {
				FieldMapper<String> mapper = MapperBuilders.stringField(mapping.getFieldName())
						.store(true)
						.index(true)
						.tokenized(!mapping.isUriField())
						.build(context);
				fieldMappers.put(mapping, mapper);
			}

			context.path().remove(); // remove name

			return new OntologyMapper(name, context, ontologySettings, fieldMappers, threadPool);
		}

		static ObjectObjectOpenHashMap<String, FieldMapper<String>> buildDynamicMappers(BuilderContext context, String relatedField) {
			ObjectObjectOpenHashMap<String, FieldMapper<String>> dynamicMappers = new ObjectObjectOpenHashMap<>();

			FieldMapper<String> uriMapper = MapperBuilders.stringField(relatedField + DYNAMIC_URI_FIELD_SUFFIX)
					.store(true)
					.index(true)
					.tokenized(false)
					.build(context);
			FieldMapper<String> labelMapper = MapperBuilders.stringField(relatedField + DYNAMIC_LABEL_FIELD_SUFFIX)
					.store(true)
					.index(true)
					.tokenized(true)
					.build(context);

			dynamicMappers.put(uriMapper.name(), uriMapper);
			dynamicMappers.put(labelMapper.name(), labelMapper);
			logger.debug("Add dynamic mappers for {}, {}", uriMapper.name(), labelMapper.name());

			return dynamicMappers;
		}

	}


	public static class TypeParser implements Mapper.TypeParser {

		private final ThreadPool threadPool;

		public TypeParser(ThreadPool tPool) {
			this.threadPool = tPool;
		}

		/**
		 * Parse the mapping definition for the ontology type.
		 *
		 * @param name the field name
		 * @param node the JSON node holding the mapping definitions.
		 * @param parserContext the parser context
		 * @return a Builder for an OntologyMapper.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public Builder parse(String name, Map<String, Object> node, ParserContext parserContext)
				throws MapperParsingException {
			OntologySettings ontologySettings = null;

			for (Entry<String, Object> entry : node.entrySet()) {
				if (entry.getKey().equals(OntologySettings.ONTOLOGY_SETTINGS_KEY)) {
					ontologySettings = parseOntologySettings((Map<String, Object>) entry.getValue());
				}
			}

			if (ontologySettings == null) {
				throw new MapperParsingException("No ontology settings supplied");
			} else if (StringUtils.isBlank(ontologySettings.getOntologyUri())
					&& StringUtils.isBlank(ontologySettings.getOlsBaseUrl())) {
				throw new MapperParsingException("No ontology URI or OLS details supplied");
			}

			return new OntologyMapper.Builder(name, ontologySettings, threadPool);
		}

		private OntologySettings parseOntologySettings(Map<String, Object> ontSettingsNode) {
			OntologySettings settings = new OntologySettings();

			for (Entry<String, Object> entry : ontSettingsNode.entrySet()) {
				String key = entry.getKey();
				switch (key) {
					case OntologySettings.ONTOLOGY_URI_PARAM:
						settings.setOntologyUri(entry.getValue().toString());
						break;
					case OntologySettings.LABEL_URI_PARAM:
						settings.setLabelPropertyUris(extractList(entry.getValue()));
						break;
					case OntologySettings.SYNONYM_URI_PARAM:
						settings.setSynonymPropertyUris(extractList(entry.getValue()));
						break;
					case OntologySettings.DEFINITION_URI_PARAM:
						settings.setDefinitionPropertyUris(extractList(entry.getValue()));
						break;
					case OntologySettings.INCLUDE_INDIRECT_PARAM:
						settings.setIncludeIndirect(Boolean.parseBoolean(entry.getValue().toString()));
						break;
					case OntologySettings.INCLUDE_RELATIONS_PARAM:
						settings.setIncludeRelations(Boolean.parseBoolean(entry.getValue().toString()));
						break;
					case OntologySettings.OLS_BASE_URL_PARAM:
						settings.setOlsBaseUrl(entry.getValue().toString());
						break;
					case OntologySettings.OLS_ONTOLOGY_PARAM:
						settings.setOlsOntology(entry.getValue().toString());
						break;
					case OntologySettings.OLS_THREADPOOL_PARAM:
						settings.setThreadpoolSize(Integer.parseInt(entry.getValue().toString()));
						break;
					case OntologySettings.OLS_PAGESIZE_PARAM:
						settings.setPageSize(Integer.parseInt(entry.getValue().toString()));
						break;
				}
			}

			return settings;
		}

		@SuppressWarnings("rawtypes")
		private List<String> extractList(Object value) {
			List<String> ret = null;

			if (value instanceof String) {
				ret = Collections.singletonList((String) value);
			} else if (value instanceof List) {
				ret = new ArrayList<>(((List) value).size());
				for (Object v : (List) value) {
					ret.add(v.toString());
				}
			}

			return ret;
		}

	}


	private final String name;
	private final OntologySettings ontologySettings;
	private volatile ImmutableOpenMap<FieldMappings, FieldMapper<String>> fieldMappers = ImmutableOpenMap.of();
	private volatile ObjectObjectOpenHashMap<String, FieldMapper<String>> dynamicFieldMappers = new ObjectObjectOpenHashMap<>();
	private final ThreadPool threadPool;
	private final BuilderContext builderContext;

	private static Map<String, OntologyHelper> helpers = new ConcurrentHashMap<>();

	public OntologyMapper(String name, BuilderContext context, OntologySettings oSettings,
						  Map<FieldMappings, FieldMapper<String>> fieldMappers,
						  ThreadPool threadPool) {
		this.name = name;
		this.ontologySettings = oSettings;
		this.fieldMappers = ImmutableOpenMap.builder(this.fieldMappers).putAll(fieldMappers).build();
		this.threadPool = threadPool;
		this.builderContext = context;
	}


	@Override
	public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
		builder.startObject(name());
		builder.field("type", RegisterOntologyType.ONTOLOGY_TYPE);

		builder.startObject(OntologySettings.ONTOLOGY_SETTINGS_KEY);
		builder.field(OntologySettings.OLS_BASE_URL_PARAM, ontologySettings.getOlsBaseUrl());
		builder.field(OntologySettings.OLS_ONTOLOGY_PARAM, ontologySettings.getOlsOntology());
		builder.field(OntologySettings.ONTOLOGY_URI_PARAM, ontologySettings.getOntologyUri());
		builder.field(OntologySettings.LABEL_URI_PARAM, ontologySettings.getLabelPropertyUris());
		builder.field(OntologySettings.DEFINITION_URI_PARAM, ontologySettings.getDefinitionPropertyUris());
		builder.field(OntologySettings.SYNONYM_URI_PARAM, ontologySettings.getSynonymPropertyUris());
		builder.field(OntologySettings.INCLUDE_INDIRECT_PARAM, ontologySettings.isIncludeIndirect());
		builder.field(OntologySettings.INCLUDE_RELATIONS_PARAM, ontologySettings.isIncludeRelations());
		builder.endObject();

		for (ObjectObjectCursor<FieldMappings, FieldMapper<String>> cursor : fieldMappers) {
			cursor.value.toXContent(builder, params);
		}

		for (ObjectObjectCursor<String, FieldMapper<String>> cursor : dynamicFieldMappers) {
			cursor.value.toXContent(builder, params);
		}

		builder.endObject();  // name

		return builder;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public void parse(ParseContext context) throws IOException {
		String iri;
		XContentParser parser = context.parser();
		XContentParser.Token token = parser.currentToken();
		if (token == XContentParser.Token.VALUE_STRING) {
			iri = parser.text();
		} else {
			throw new MapperParsingException(name() + " does not contain String value");
		}

		try {
			OntologyHelper helper = getHelper(ontologySettings, threadPool);

			OntologyData data = findOntologyData(helper, iri);
			if (data == null) {
				logger.debug("Cannot find OWL class for IRI {}", iri);
			} else {
				// We add the data as an external value, then use mapper.parse() to
				// add the field to the record. We don't need to explicitly add the
				// field - the mapper handles that.
				context.externalValue(iri);
				fieldMappers.get(FieldMappings.URI).parse(context);

				// Look up the label(s)
				Collection<String> labels = data.getLabels();
				for (String label : labels) {
					context.externalValue(label);
					fieldMappers.get(FieldMappings.LABEL).parse(context);
				}

				// Look up the synonyms
				for (String synonym : data.getSynonyms()) {
					context.externalValue(synonym);
					fieldMappers.get(FieldMappings.SYNONYMS).parse(context);
				}

				// Add the child details
				addRelatedNodesWithLabels(data.getChildIris(), data.getChildLabels(), context,
						fieldMappers.get(FieldMappings.CHILD_URI), fieldMappers.get(FieldMappings.CHILD_LABEL));

				// Add the parent details
				addRelatedNodesWithLabels(data.getParentIris(), data.getParentLabels(), context,
						fieldMappers.get(FieldMappings.PARENT_URI), fieldMappers.get(FieldMappings.PARENT_LABEL));

				if (ontologySettings.isIncludeIndirect()) {
					// Add the descendant details
					addRelatedNodesWithLabels(data.getDescendantIris(), data.getDescendantLabels(), context,
							fieldMappers.get(FieldMappings.DESCENDANT_URI), fieldMappers.get(FieldMappings.DESCENDANT_LABEL));

					// Add the ancestor details
					addRelatedNodesWithLabels(data.getAncestorIris(), data.getAncestorLabels(), context,
							fieldMappers.get(FieldMappings.ANCESTOR_URI), fieldMappers.get(FieldMappings.ANCESTOR_LABEL));
				}

				if (ontologySettings.isIncludeRelations()) {
					// Add the related nodes
					Map<String, Collection<String>> relations = data.getRelationIris();

					for (String relation : relations.keySet()) {
						String sanRelation = relation.replaceAll("\\W+", "_");
						String uriMapperName = sanRelation + DYNAMIC_URI_FIELD_SUFFIX;
						String labelMapperName = sanRelation + DYNAMIC_LABEL_FIELD_SUFFIX;

						if (!dynamicFieldMappers.containsKey(uriMapperName)) {
							builderContext.path().add(name);
							dynamicFieldMappers.putAll(Builder.buildDynamicMappers(builderContext, sanRelation));
							builderContext.path().remove();
						}

						FieldMapper<String> uriMapper = dynamicFieldMappers.get(uriMapperName);
						FieldMapper<String> labelMapper = dynamicFieldMappers.get(labelMapperName);

						if (uriMapper == null) {
							logger.warn("No mapper found for dynamic field {} - ignoring", relation);
							continue;
						}

						// Add the URI fields
						for (String relIri : relations.get(relation)) {
							context.externalValue(relIri);
							uriMapper.parse(context);
						}

						// Add the labels
						for (String label : helper.findLabelsForIRIs(relations.get(relation))) {
							context.externalValue(label);
							labelMapper.parse(context);
						}
					}
				}
			}

			helper.updateLastCallTime();
		} catch (OntologyHelperException e) {
			throw new ElasticsearchException("Could not initialise ontology helper", e);
		}
	}

	private OntologyData findOntologyData(OntologyHelper helper, String iri) {
		OntologyData data = null;
		try {
			data = new OntologyDataBuilder(helper, iri)
					.includeSynonyms(true)
					.includeDefinitions(true)
					.includeIndirect(ontologySettings.isIncludeIndirect())
					.includeRelations(ontologySettings.isIncludeRelations())
					.build();
		} catch (OntologyHelperException e) {
			logger.error("Problem building ontology data for {}: {}", iri, e.getMessage());
		}
		return data;
	}

	private void addRelatedNodesWithLabels(Collection<String> iris, Collection<String> labels, ParseContext context,
										   FieldMapper<String> iriMapper, FieldMapper<String> labelMapper) throws IOException {
		if (!iris.isEmpty()) {
			for (String iri : iris) {
				context.externalValue(iri);
				iriMapper.parse(context);
			}
			for (String label : labels) {
				context.externalValue(label);
				labelMapper.parse(context);
			}
		}
	}

	@Override
	public void merge(Mapper mergeWith, MergeContext mergeContext) throws MergeMappingException {
	}

	@Override
	public void traverse(FieldMapperListener fieldMapperListener) {
		for (ObjectObjectCursor<FieldMappings, FieldMapper<String>> cursor : fieldMappers) {
			cursor.value.traverse(fieldMapperListener);
		}
		for (ObjectObjectCursor<String, FieldMapper<String>> cursor : dynamicFieldMappers) {
			cursor.value.traverse(fieldMapperListener);
		}
	}

	@Override
	public void traverse(ObjectMapperListener objectMapperListener) {
	}

	@Override
	public void close() {
		for (ObjectObjectCursor<FieldMappings, FieldMapper<String>> cursor : fieldMappers) {
			cursor.value.close();
		}
//		disposeHelper();
	}


	public static OntologyHelper getHelper(OntologySettings settings, ThreadPool threadPool) throws OntologyHelperException {
		String helperKey = buildHelperKey(settings);
		OntologyHelper helper = helpers.get(helperKey);

		if (helper == null) {
			helper = new ElasticOntologyHelperFactory(settings).buildOntologyHelper();
			OntologyCheckRunnable checker = new OntologyCheckRunnable(settings.getOntologyUri());
			threadPool.scheduleWithFixedDelay(checker, TimeValue.timeValueMillis(DELETE_CHECK_DELAY_MS));
			helpers.put(helperKey, helper);
			helper.updateLastCallTime();
		}

		return helper;
	}

	private static String buildHelperKey(OntologySettings settings) {
		String key;

		if (StringUtils.isNotBlank(settings.getOntologyUri())) {
			key = settings.getOntologyUri();
		} else {
			if (StringUtils.isNotBlank(settings.getOlsOntology())) {
				key = settings.getOlsBaseUrl() + "_" + settings.getOlsOntology();
			} else {
				key = settings.getOlsBaseUrl();
			}
		}

		return key;
	}


	private static final class OntologyCheckRunnable implements Runnable {

		final String ontologyUri;

		public OntologyCheckRunnable(String ontologyUri) {
			this.ontologyUri = ontologyUri;
		}

		@Override
		public void run() {
			OntologyHelper helper = helpers.get(ontologyUri);
			if (helper != null) {
				// Check if the last call time was longer ago than the maximum
				if (System.currentTimeMillis() - DELETE_CHECK_DELAY_MS > helper.getLastCallTime()) {
					// Assume helper is out of use - dispose of it to allow memory to be freed
					helper.dispose();
					helpers.remove(ontologyUri);
				}
			}
		}

	}

}
