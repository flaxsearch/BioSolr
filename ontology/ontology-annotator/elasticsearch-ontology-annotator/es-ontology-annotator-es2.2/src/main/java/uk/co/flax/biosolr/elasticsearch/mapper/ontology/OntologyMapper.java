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

import com.google.common.collect.Iterators;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Field;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.collect.CopyOnWriteHashMap;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.*;
import org.elasticsearch.index.mapper.core.StringFieldMapper;
import uk.co.flax.biosolr.elasticsearch.OntologyHelperBuilder;
import uk.co.flax.biosolr.ontology.core.OntologyData;
import uk.co.flax.biosolr.ontology.core.OntologyDataBuilder;
import uk.co.flax.biosolr.ontology.core.OntologyHelper;
import uk.co.flax.biosolr.ontology.core.OntologyHelperException;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static org.elasticsearch.index.mapper.core.TypeParsers.parseField;

/**
 * Mapper class to expand ontology details from an ontology
 * annotation field value.
 *
 * @author mlp
 */
public class OntologyMapper extends FieldMapper {

	public static final String CONTENT_TYPE = "ontology";

	public static final String ONTOLOGY_PROPERTIES = "properties";

	public static final String DYNAMIC_URI_FIELD_SUFFIX = "_rel_uris";
	public static final String DYNAMIC_LABEL_FIELD_SUFFIX = "_rel_labels";

	private static final ESLogger logger = ESLoggerFactory.getLogger(OntologyMapper.class.getName());

	public static class Defaults {
		public static final ContentPath.Type PATH_TYPE = ContentPath.Type.FULL;
		public static final MappedFieldType LABEL_FIELD_TYPE = new StringFieldMapper.StringFieldType();
		public static final MappedFieldType URI_FIELD_TYPE = new StringFieldMapper.StringFieldType();

		public static final MappedFieldType FIELD_TYPE = new StringFieldMapper.StringFieldType();

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


	public static class Builder extends FieldMapper.Builder<Builder, OntologyMapper> {

		private final ContentPath.Type pathType = Defaults.PATH_TYPE;

		private OntologySettings ontologySettings;
		private Map<String, StringFieldMapper.Builder> propertyBuilders;

		public Builder(String name) {
			super(name, Defaults.FIELD_TYPE, Defaults.FIELD_TYPE);
			builder = this;
		}

		public Builder ontologySettings(OntologySettings settings) {
			this.ontologySettings = settings;
			return this;
		}

		public Builder propertyBuilders(Map<String, StringFieldMapper.Builder> props) {
			this.propertyBuilders = props;
			return this;
		}

		@Override
		public OntologyMapper build(BuilderContext context) {
			ContentPath.Type origPathType = context.path().pathType();
			context.path().pathType(pathType);

			Map<String, StringFieldMapper> fieldMappers = new HashMap<>();

			context.path().add(name);

			if (propertyBuilders != null) {
				for (String property : propertyBuilders.keySet()) {
					StringFieldMapper sfm = propertyBuilders.get(property).build(context);
					fieldMappers.put(sfm.fieldType().names().indexName(), sfm);
				}
			}

			// Initialise field mappers for the pre-defined fields
			for (FieldMappings mapping : ontologySettings.getFieldMappings()) {
				if (!fieldMappers.containsKey(context.path().fullPathAsText(mapping.getFieldName()))) {
					StringFieldMapper mapper = MapperBuilders.stringField(mapping.getFieldName())
							.store(true)
							.index(true)
							.tokenized(!mapping.isUriField())
							.includeInAll(true)
							.build(context);
					fieldMappers.put(mapper.fieldType().names().indexName(), mapper);
				}
			}

			context.path().remove(); // remove name
			context.path().pathType(origPathType);

			setupFieldType(context);

			return new OntologyMapper(name, fieldType, defaultFieldType, context.indexSettings(),
					multiFieldsBuilder.build(this, context),
					ontologySettings, fieldMappers);
		}

	}


	public static class TypeParser implements Mapper.TypeParser {

		/**
		 * Parse the mapping definition for the ontology type.
		 *
		 * @param name          the field name
		 * @param node          the JSON node holding the mapping definitions.
		 * @param parserContext the parser context object.
		 * @return a Builder for an OntologyMapper.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public Builder parse(String name, Map<String, Object> node, ParserContext parserContext)
				throws MapperParsingException {
			OntologySettings ontologySettings = null;

			Builder builder = new Builder(name);
			parseField(builder, name, node, parserContext);

			for (Iterator<Entry<String, Object>> iterator = node.entrySet().iterator(); iterator.hasNext(); ) {
				Entry<String, Object> entry = iterator.next();
				if (entry.getKey().equals(OntologySettings.ONTOLOGY_SETTINGS_KEY)) {
					ontologySettings = new OntologySettingsBuilder()
							.settingsNode((Map<String, Object>) entry.getValue())
							.build();
					iterator.remove();
				} else if (entry.getKey().equals(ONTOLOGY_PROPERTIES)) {
					Map<String, StringFieldMapper.Builder> builders = parseProperties((Map<String, Object>) entry.getValue(), parserContext);
					builder = builder.propertyBuilders(builders);
					iterator.remove();
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

		private Map<String, StringFieldMapper.Builder> parseProperties(Map<String, Object> propertiesNode, ParserContext parserContext) {
			Map<String, StringFieldMapper.Builder> propertyMap = new HashMap<>();
			for (Iterator<Entry<String, Object>> iterator = propertiesNode.entrySet().iterator(); iterator.hasNext(); ) {
				Entry<String, Object> entry = iterator.next();
				String name = entry.getKey();

				@SuppressWarnings("unchecked")
				Mapper.Builder builder = new StringFieldMapper.TypeParser().parse(entry.getKey(), (Map<String, Object>) entry.getValue(), parserContext);
				propertyMap.put(name, (StringFieldMapper.Builder) builder);
			}
			return propertyMap;
		}

	}


	private final Object mutex = new Object();

	private final OntologySettings ontologySettings;
	private volatile CopyOnWriteHashMap<String, StringFieldMapper> mappers;

	public OntologyMapper(String simpleName, MappedFieldType fieldType, MappedFieldType defaultFieldType,
			Settings indexSettings, MultiFields multiFields, OntologySettings oSettings,
			Map<String, StringFieldMapper> fieldMappers) {
		super(simpleName, fieldType, defaultFieldType, indexSettings, multiFields, null);
		this.ontologySettings = oSettings;
		// Dynamic mappers are added to mappers map as they are used/created
		this.mappers = CopyOnWriteHashMap.copyOf(fieldMappers);
	}

	@Override
	public String contentType() {
		return CONTENT_TYPE;
	}

	@Override
	protected void parseCreateField(ParseContext context, List<Field> fields) throws IOException {
		throw new UnsupportedOperationException(
				"Parsing is implemented in parse(), this method should NEVER be called");
	}

	@Override
	public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
		builder.startObject(name());
		builder.field("type", CONTENT_TYPE);

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
		builder.field(OntologySettings.INCLUDE_PARENT_PATHS_PARAM, ontologySettings.isIncludeParentPaths());
		builder.field(OntologySettings.INCLUDE_PARENT_PATH_LABELS_PARAM, ontologySettings.isIncludeParentPathLabels());
		builder.endObject();

		if (!mappers.isEmpty()) {
			Mapper[] sortedMappers = mappers.values().toArray(new Mapper[mappers.size()]);
			Arrays.sort(sortedMappers, new Comparator<Mapper>() {
				@Override
				public int compare(Mapper o1, Mapper o2) {
					return o1.name().compareTo(o2.name());
				}
			});
			builder.startObject(ONTOLOGY_PROPERTIES);
			for (Mapper mapper : sortedMappers) {
				mapper.toXContent(builder, params);
			}
			builder.endObject();  // ontology_properties
		}

		builder.endObject();  // name

		return builder;
	}

	@Override
	public Mapper parse(ParseContext context) throws IOException {
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
		context.path().add(simpleName());

		boolean modified = false;

		try {
			OntologyHelper helper = OntologyHelperBuilder.getOntologyHelper(ontologySettings);

			OntologyData data = findOntologyData(helper, iri);
			if (data == null) {
				logger.debug("Cannot find OWL class for IRI {}", iri);
			} else {
				// Add the IRI
				addFieldData(context, getPredefinedMapper(FieldMappings.URI, context), Collections.singletonList(iri));

				// Look up the label(s)
				addFieldData(context, getPredefinedMapper(FieldMappings.LABEL, context), data.getLabels());

				// Look up the synonyms
				addFieldData(context, getPredefinedMapper(FieldMappings.SYNONYMS, context), data.getLabels());

				// Add the child details
				addRelatedNodesWithLabels(context, data.getChildIris(), getPredefinedMapper(FieldMappings.CHILD_URI, context), data.getChildLabels(),
						getPredefinedMapper(FieldMappings.CHILD_LABEL, context));

				// Add the parent details
				addRelatedNodesWithLabels(context, data.getParentIris(), getPredefinedMapper(FieldMappings.PARENT_URI, context), data.getParentLabels(),
						getPredefinedMapper(FieldMappings.PARENT_LABEL, context));

				if (ontologySettings.isIncludeIndirect()) {
					// Add the descendant details
					addRelatedNodesWithLabels(context, data.getDescendantIris(), getPredefinedMapper(FieldMappings.DESCENDANT_URI, context), data.getDescendantLabels(),
							getPredefinedMapper(FieldMappings.DESCENDANT_LABEL, context));

					// Add the ancestor details
					addRelatedNodesWithLabels(context, data.getAncestorIris(), getPredefinedMapper(FieldMappings.ANCESTOR_URI, context), data.getAncestorLabels(),
							getPredefinedMapper(FieldMappings.ANCESTOR_LABEL, context));
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
						StringFieldMapper uriMapper = mappers.get(context.path().fullPathAsText(uriMapperName));
						StringFieldMapper labelMapper = mappers.get(context.path().fullPathAsText(labelMapperName));

						if (uriMapper == null) {
							// No mappers created yet - build new ones for URI and label
							BuilderContext builderContext = new BuilderContext(context.indexSettings(), context.path());
							uriMapper = MapperBuilders.stringField(uriMapperName)
									.store(true)
									.index(true)
									.tokenized(false)
									.includeInAll(true)
									.build(builderContext);
							labelMapper = MapperBuilders.stringField(labelMapperName)
									.store(true)
									.index(true)
									.tokenized(true)
									.includeInAll(true)
									.build(builderContext);

							synchronized (mutex) {
								mappers = mappers.copyAndPut(uriMapper.fieldType().names().indexName(), uriMapper);
								mappers = mappers.copyAndPut(labelMapper.fieldType().names().indexName(), labelMapper);
							}
							modified = true;
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

		return modified ? this : null;
	}

	private StringFieldMapper getPredefinedMapper(FieldMappings mapping, ParseContext context) {
		String mappingName = context.path().fullPathAsText(mapping.getFieldName());
		return mappers.get(mappingName);
	}

	private OntologyData findOntologyData(OntologyHelper helper, String iri) {
		OntologyData data = null;
		try {
			data = new OntologyDataBuilder(helper, iri)
					.includeSynonyms(true)
					.includeDefinitions(true)
					.includeIndirect(ontologySettings.isIncludeIndirect())
					.includeRelations(ontologySettings.isIncludeRelations())
					.includeParentPaths(ontologySettings.isIncludeParentPaths())
					.includeParentPathLabels(ontologySettings.isIncludeParentPathLabels())
					.build();
		} catch (OntologyHelperException e) {
			logger.error("Problem building ontology data for {}: {}", iri, e.getMessage());
		}
		return data;
	}

	private void addFieldData(ParseContext context, StringFieldMapper mapper, Collection<String> data) throws IOException {
		if (data != null && !data.isEmpty()) {
			for (String value : data) {
				ParseContext evc = context.createExternalValueContext(value);
				mapper.parse(evc);
			}
		}
	}

	private void addRelatedNodesWithLabels(ParseContext context, Collection<String> iris, StringFieldMapper iriMapper,
			Collection<String> labels, StringFieldMapper labelMapper) throws IOException {
		if (!iris.isEmpty()) {
			addFieldData(context, iriMapper, iris);
			addFieldData(context, labelMapper, labels);
		}
	}

	public OntologyMapper clone() {
		// Shallow clone
		return (OntologyMapper) super.clone();
	}

	@Override
	public void doMerge(Mapper mergeWith, boolean includeInAll) throws MergeMappingException {
		super.doMerge(mergeWith, includeInAll);

		OntologyMapper oMergeWith = (OntologyMapper) mergeWith;
		OntologySettings mergeSettings = oMergeWith.ontologySettings;
		if (mergeSettings.getOntologyUri() != null && !mergeSettings.getOntologyUri().equals(ontologySettings.getOntologyUri())) {
			throw new IllegalArgumentException("mapper [" + fieldType().names().fullName() + "] has different ontology URI");
		} else if (mergeSettings.getOlsBaseUrl() != null && !mergeSettings.getOlsBaseUrl().equals(ontologySettings.getOlsBaseUrl())) {
			throw new IllegalArgumentException("mapper [" + fieldType().names().fullName() + "] has different OLS base URL");
		}

		// Merge the mappers
		List<FieldMapper> newFieldMappers = null;
		Map<String, StringFieldMapper> newMapperMap = null;

		for (Entry<String, StringFieldMapper> entry : oMergeWith.mappers.entrySet()) {
			StringFieldMapper mergeIntoMapper = mappers.get(entry.getKey());
			if (mergeIntoMapper == null) {
				if (newFieldMappers == null) {
					newFieldMappers = new ArrayList<>(oMergeWith.mappers.size());
					newMapperMap = new HashMap<>();
				}
				newFieldMappers.add(entry.getValue());
				newMapperMap.put(entry.getKey(), entry.getValue());
			} else {
				mergeIntoMapper.merge(entry.getValue(), includeInAll);
			}
		}

		if (newFieldMappers != null) {
			mappers = mappers.copyAndPutAll(newMapperMap);
		}
	}

	@Override
	public Iterator<Mapper> iterator() {
		List<Mapper> extras = new ArrayList<>(mappers.values());
		return Iterators.concat(super.iterator(), extras.iterator());
	}

	@Override
	public boolean isGenerated() {
		return true;
	}

}
