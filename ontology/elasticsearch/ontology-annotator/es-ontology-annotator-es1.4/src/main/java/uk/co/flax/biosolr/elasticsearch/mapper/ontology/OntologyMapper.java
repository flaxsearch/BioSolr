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

package uk.co.flax.biosolr.elasticsearch.mapper.ontology;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.collect.UpdateInPlaceMap;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.netty.util.internal.ConcurrentHashMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.analysis.NamedAnalyzer;
import org.elasticsearch.index.codec.docvaluesformat.DocValuesFormatProvider;
import org.elasticsearch.index.codec.postingsformat.PostingsFormatProvider;
import org.elasticsearch.index.fielddata.FieldDataType;
import org.elasticsearch.index.mapper.*;
import org.elasticsearch.index.mapper.core.AbstractFieldMapper;
import org.elasticsearch.index.similarity.SimilarityProvider;
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
public class OntologyMapper extends AbstractFieldMapper<OntologyData> {

	public static final long DELETE_CHECK_DELAY_MS = 15 * 60 * 1000; // 15 minutes

	public static final String ONTOLOGY_PROPERTIES = "ontology_properties";

	public static final String DYNAMIC_URI_FIELD_SUFFIX = "_rel_uris";
	public static final String DYNAMIC_LABEL_FIELD_SUFFIX = "_rel_labels";

    private static final ESLogger logger = ESLoggerFactory.getLogger(OntologyMapper.class.getName());


	public static class Defaults extends AbstractFieldMapper.Defaults {
		public static final FieldType LABEL_FIELD_TYPE = new FieldType(AbstractFieldMapper.Defaults.FIELD_TYPE);
		public static final FieldType URI_FIELD_TYPE = new FieldType(AbstractFieldMapper.Defaults.FIELD_TYPE);

		public static final FieldType FIELD_TYPE = new FieldType(AbstractFieldMapper.Defaults.FIELD_TYPE);

		static {
			LABEL_FIELD_TYPE.setStored(true);
			LABEL_FIELD_TYPE.setTokenized(true);
			LABEL_FIELD_TYPE.freeze();

			URI_FIELD_TYPE.setStored(true);
			URI_FIELD_TYPE.setTokenized(false);
			URI_FIELD_TYPE.freeze();

			FIELD_TYPE.setStored(true);
			FIELD_TYPE.freeze();
		}
	}


	public static class Builder extends AbstractFieldMapper.Builder<Builder, OntologyMapper> {

		private ContentPath.Type pathType = Defaults.PATH_TYPE;

		private OntologySettings ontologySettings;
		private final ThreadPool threadPool;

		public Builder(String name, ThreadPool threadPool) {
			super(name, Defaults.FIELD_TYPE);
			this.threadPool = threadPool;
		}

		public Builder ontologySettings(OntologySettings settings) {
			this.ontologySettings = settings;
			return this;
		}

		@Override
		public OntologyMapper build(BuilderContext context) {
			ContentPath.Type origPathType = context.path().pathType();
			context.path().pathType(pathType);

			Map<FieldMappings, FieldMapper<String>> fieldMappers = Maps.newHashMap();

			context.path().add(name);

			// Initialise field mappers for the pre-defined fields
			for (FieldMappings mapping : FieldMappings.values()) {
				FieldMapper<String> mapper = MapperBuilders.stringField(mapping.getFieldName())
						.store(true)
						.index(true)
						.tokenized(!mapping.isUriField())
						.build(context);
				fieldMappers.put(mapping, mapper);
			}

			context.path().remove(); // remove name
			context.path().pathType(origPathType);

			return new OntologyMapper(buildNames(context), fieldType, docValues, indexAnalyzer, searchAnalyzer,
					postingsProvider, docValuesProvider,
					similarity, fieldDataSettings, context.indexSettings(),
					new MultiFields.Builder().build(this, context),
					ontologySettings, fieldMappers, threadPool);
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
		 * @param parserContext the parser context object.
		 * @return a Builder for an OntologyMapper.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public Builder parse(String name, Map<String, Object> node, ParserContext parserContext)
				throws MapperParsingException {
			OntologySettings ontologySettings = null;

			Builder builder = new Builder(name, threadPool);

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
			} else {
				builder = builder.ontologySettings(ontologySettings);
			}
			
			return builder;
		}

		private OntologySettings parseOntologySettings(Map<String, Object> ontSettingsNode) {
			OntologySettings settings = new OntologySettings();

			for (Entry<String, Object> entry : ontSettingsNode.entrySet()) {
				String key = entry.getKey();
				if (entry.getValue() != null) {
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
			}

			return settings;
		}

		@SuppressWarnings("rawtypes")
		private List<String> extractList(Object value) {
			List<String> ret = null;

			if (value instanceof String) {
				ret = Collections.singletonList((String) value);
			} else if (value instanceof List) {
				ret = new ArrayList<>(((List)value).size());
				for (Object v : (List)value) {
					ret.add(v.toString());
				}
			}

			return ret;
		}

	}


	private final Object mutex = new Object();

	private final OntologySettings ontologySettings;
	private volatile ImmutableOpenMap<FieldMappings, FieldMapper<String>> predefinedMappers = ImmutableOpenMap.of();
	private final UpdateInPlaceMap<String, FieldMapper<String>> mappers;
	private final ThreadPool threadPool;
	
	private static Map<String, OntologyHelper> helpers = new ConcurrentHashMap<>();

	public OntologyMapper(FieldMapper.Names names, FieldType fieldType, Boolean docValues,
			NamedAnalyzer indexAnalyzer, NamedAnalyzer searchAnalyzer,
			PostingsFormatProvider postingsFormat, DocValuesFormatProvider docValuesFormat,
			SimilarityProvider similarity, @Nullable Settings fieldDataSettings, Settings indexSettings, MultiFields multiFields, OntologySettings oSettings,
			Map<FieldMappings, FieldMapper<String>> fieldMappers,
			ThreadPool threadPool) {
		super(names, 1f, fieldType, docValues, searchAnalyzer, indexAnalyzer, postingsFormat, docValuesFormat, similarity, null,
				fieldDataSettings, indexSettings, multiFields, null);
		this.ontologySettings = oSettings;
		// Predefined mappers are already defined, not dynamic;
		// mappers for additional relations are created as required.
		this.predefinedMappers = ImmutableOpenMap.builder(predefinedMappers).putAll(fieldMappers).build();
		// Mappers are added to mappers map as they are used/created
		this.mappers = UpdateInPlaceMap.of(MapperService.getFieldMappersCollectionSwitch(indexSettings));
		this.threadPool = threadPool;
	}


	@Override
	public String contentType() {
		return RegisterOntologyType.ONTOLOGY_TYPE;
	}

	@Override
	public FieldType defaultFieldType() {
		return Defaults.FIELD_TYPE;
	}

	@Override
	public FieldDataType defaultFieldDataType() {
		return new FieldDataType(RegisterOntologyType.ONTOLOGY_TYPE);
	}

	@Override
	protected void parseCreateField(ParseContext context, List<Field> fields) throws IOException {
		throw new UnsupportedOperationException(
				"Parsing is implemented in parse(), this method should NEVER be called");
	}

	@Override
	public OntologyData value(Object value) {
		if (value instanceof OntologyData) {
			return (OntologyData) value;
		}
		return null;
	}

	@Override
	public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
		builder.startObject(name());
		builder.field("type", RegisterOntologyType.ONTOLOGY_TYPE);

		builder.startObject(OntologySettings.ONTOLOGY_SETTINGS_KEY);
		if (StringUtils.isNotBlank(ontologySettings.getOntologyUri())) {
			builder.field(OntologySettings.ONTOLOGY_URI_PARAM, ontologySettings.getOntologyUri());
			builder.field(OntologySettings.LABEL_URI_PARAM, ontologySettings.getLabelPropertyUris());
			builder.field(OntologySettings.DEFINITION_URI_PARAM, ontologySettings.getDefinitionPropertyUris());
			builder.field(OntologySettings.SYNONYM_URI_PARAM, ontologySettings.getSynonymPropertyUris());
		}
		if (StringUtils.isNotBlank(ontologySettings.getOlsBaseUrl())) {
			builder.field(OntologySettings.OLS_BASE_URL_PARAM, ontologySettings.getOlsBaseUrl());
			builder.field(OntologySettings.OLS_ONTOLOGY_PARAM, ontologySettings.getOlsOntology());
		}
		builder.field(OntologySettings.INCLUDE_INDIRECT_PARAM, ontologySettings.isIncludeIndirect());
		builder.field(OntologySettings.INCLUDE_RELATIONS_PARAM, ontologySettings.isIncludeRelations());
		builder.endObject();

		builder.startObject(ONTOLOGY_PROPERTIES);
		for (Mapper mapper : sortMappers()) {
			mapper.toXContent(builder, params);
		}
		builder.endObject();  // ontology_properties

		builder.endObject();  // name

		return builder;
	}

	private Set<Mapper> sortMappers() {
		SortedSet<Mapper> sortedMappers = new TreeSet<>(new Comparator<Mapper>() {
			@Override
			public int compare(Mapper o1, Mapper o2) {
				return o1.name().compareTo(o2.name());
			}
		});
		for (Mapper m : mappers.values()) {
			sortedMappers.add(m);
		}
		return sortedMappers;
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

		ContentPath.Type origPathType = context.path().pathType();
		context.path().pathType(ContentPath.Type.FULL);
		context.path().add(names.name());

		try {
			OntologyHelper helper = getHelper(ontologySettings, threadPool);

			OntologyData data = findOntologyData(helper, iri);
			if (data == null) {
				logger.debug("Cannot find OWL class for IRI {}", iri);
			} else {
				addFieldData(context, predefinedMappers.get(FieldMappings.URI), Collections.singletonList(iri));

				// Look up the label(s)
				addFieldData(context, predefinedMappers.get(FieldMappings.LABEL), data.getLabels());

				// Look up the synonyms
				addFieldData(context, predefinedMappers.get(FieldMappings.SYNONYMS), data.getLabels());

				// Add the child details
				addRelatedNodesWithLabels(context, data.getChildIris(), predefinedMappers.get(FieldMappings.CHILD_URI), data.getChildLabels(),
						predefinedMappers.get(FieldMappings.CHILD_LABEL));

				// Add the parent details
				addRelatedNodesWithLabels(context, data.getParentIris(), predefinedMappers.get(FieldMappings.PARENT_URI), data.getParentLabels(),
						predefinedMappers.get(FieldMappings.PARENT_LABEL));

				if (ontologySettings.isIncludeIndirect()) {
					// Add the descendant details
					addRelatedNodesWithLabels(context, data.getDescendantIris(), predefinedMappers.get(FieldMappings.DESCENDANT_URI), data.getDescendantLabels(),
							predefinedMappers.get(FieldMappings.DESCENDANT_LABEL));

					// Add the ancestor details
					addRelatedNodesWithLabels(context, data.getAncestorIris(), predefinedMappers.get(FieldMappings.ANCESTOR_URI), data.getAncestorLabels(),
							predefinedMappers.get(FieldMappings.ANCESTOR_LABEL));
				}
				
				if (ontologySettings.isIncludeRelations()) {
					// Add the related nodes
					Map<String, Collection<String>> relations = data.getRelationIris();

					for (String relation : relations.keySet()) {
						// Sanitise the relation name
						String sanRelation = relation.replaceAll("\\W+", "_");
						String uriMapperName = sanRelation + DYNAMIC_URI_FIELD_SUFFIX;
						String labelMapperName = sanRelation + DYNAMIC_LABEL_FIELD_SUFFIX;

						// Get the mapper for the relation
						FieldMapper<String> uriMapper = mappers.get(names.name() + "." + uriMapperName);
						FieldMapper<String> labelMapper = mappers.get(names.name() + "." + labelMapperName);

						if (uriMapper == null) {
							// No mappers created yet - build new ones for URI and label
							BuilderContext builderContext = new BuilderContext(context.indexSettings(), context.path());
							uriMapper = MapperBuilders.stringField(uriMapperName)
									.store(true)
									.index(true)
									.tokenized(false)
									.build(builderContext);
							labelMapper = MapperBuilders.stringField(labelMapperName)
									.store(true)
									.index(true)
									.tokenized(true)
									.build(builderContext);
						}

						addRelatedNodesWithLabels(context, relations.get(relation), uriMapper,
								helper.findLabelsForIRIs(relations.get(relation)), labelMapper);
					}
				}
			}

			helper.updateLastCallTime();
		} catch (OntologyHelperException e) {
			throw new ElasticsearchException("Could not initialise ontology helper", e);
		} finally {
			context.path().remove();
			context.path().pathType(origPathType);
		}
	}

	private OntologyData findOntologyData(uk.co.flax.biosolr.ontology.core.OntologyHelper helper, String iri) {
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

	private void addFieldData(ParseContext context, FieldMapper<String> mapper, Collection<String> data) throws IOException {
		if (data != null && !data.isEmpty()) {
			if (mappers.get(mapper.names().indexName()) == null) {
				// New mapper
				context.setWithinNewMapper();
				try {
					parseData(context, mapper, data);

					FieldMapperListener.Aggregator newFields = new FieldMapperListener.Aggregator();
					ObjectMapperListener.Aggregator newObjects = new ObjectMapperListener.Aggregator();
					mapper.traverse(newFields);
					mapper.traverse(newObjects);
					// callback on adding those fields!
					context.docMapper().addFieldMappers(newFields.mappers);
					context.docMapper().addObjectMappers(newObjects.mappers);

					context.setMappingsModified();

					synchronized (mutex) {
						UpdateInPlaceMap<String, FieldMapper<String>>.Mutator mappingMutator = this.mappers.mutator();
						mappingMutator.put(mapper.names().indexName(), mapper);
						mappingMutator.close();
					}
				} finally {
					context.clearWithinNewMapper();
				}
			} else {
				// Mapper already added
				parseData(context, mapper, data);
			}
		}
	}

	private void parseData(ParseContext context, FieldMapper<String> mapper, Collection<String> values) throws IOException {
		for (String value : values) {
			Field field = new Field(mapper.names().indexName(), value,
					isUriField(mapper.name()) ? Defaults.URI_FIELD_TYPE : Defaults.LABEL_FIELD_TYPE);
			context.doc().add(field);
		}
	}

	private boolean isUriField(String fieldName) {
		return fieldName.endsWith("uri") || fieldName.endsWith("uris");
	}

	private void addRelatedNodesWithLabels(ParseContext context, Collection<String> iris, FieldMapper<String> iriMapper, Collection<String> labels,
			FieldMapper<String> labelMapper) throws IOException {
		if (!iris.isEmpty()) {
			addFieldData(context, iriMapper, iris);
			addFieldData(context, labelMapper, labels);
		}
	}

	@Override
	public void merge(Mapper mergeWith, MergeContext mergeContext) throws MergeMappingException {
		super.merge(mergeWith, mergeContext);
		if (!this.getClass().equals(mergeWith.getClass())) {
			return;
		}
		OntologySettings mergeSettings = ((OntologyMapper) mergeWith).ontologySettings;
		if (mergeSettings.getOntologyUri() != null && !mergeSettings.getOntologyUri().equals(ontologySettings.getOntologyUri())) {
			mergeContext.addConflict("mapper [" + names.fullName() + "] has different ontology URI");
		} else if (mergeSettings.getOlsBaseUrl() != null && !mergeSettings.getOlsBaseUrl().equals(ontologySettings.getOlsBaseUrl())) {
			mergeContext.addConflict("mapper [" + names.fullName() + "] has different OLS base URL");
		}
	}

	@Override
	public void traverse(FieldMapperListener fieldMapperListener) {
		for (FieldMapper<String> mapper : mappers.values()) {
			mapper.traverse(fieldMapperListener);
		}
	}

	@Override
	public void traverse(ObjectMapperListener objectMapperListener) {
	}

	@Override
	public void close() {
		for (FieldMapper<String> mapper : mappers.values()) {
			mapper.close();
		}
//		disposeHelper();
	}


	public static OntologyHelper getHelper(OntologySettings settings, ThreadPool threadPool) throws OntologyHelperException {
		String helperKey = buildHelperKey(settings);
		OntologyHelper helper = helpers.get(helperKey);

		if (helper == null) {
			helper = new ElasticOntologyHelperFactory(settings).buildOntologyHelper();
			OntologyCheckRunnable checker = new OntologyCheckRunnable(helperKey);
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

		final String threadKey;

		public OntologyCheckRunnable(String threadKey) {
			this.threadKey = threadKey;
		}

		@Override
		public void run() {
			OntologyHelper helper = helpers.get(threadKey);
			if (helper != null) {
				// Check if the last call time was longer ago than the maximum
				if (System.currentTimeMillis() - DELETE_CHECK_DELAY_MS > helper.getLastCallTime()) {
					// Assume helper is out of use - dispose of it to allow memory to be freed
					helper.dispose();
					helpers.remove(threadKey);
				}
			}
		}

	}

}
