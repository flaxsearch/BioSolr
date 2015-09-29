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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.netty.util.internal.ConcurrentHashMap;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.FieldMapperListener;
import org.elasticsearch.index.mapper.Mapper;
import org.elasticsearch.index.mapper.MapperBuilders;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.mapper.MergeContext;
import org.elasticsearch.index.mapper.MergeMappingException;
import org.elasticsearch.index.mapper.ObjectMapperListener;
import org.elasticsearch.index.mapper.ParseContext;
import org.elasticsearch.threadpool.ThreadPool;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 * Mapper class to expand ontology details from an ontology
 * annotation field value.
 *
 * @author mlp
 */
public class OntologyMapper implements Mapper {

	public static final long DELETE_CHECK_DELAY_MS = 15 * 60 * 1000; // 15 minutes
	
	public static final String RELATION_SUFFIX = "_rel";
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
			
			// And build the dynamic field mappers
			Map<String, FieldMapper<String>> dynamicMappers = Maps.newHashMap();
			try {
				OntologyHelper helper = getHelper(ontologySettings, threadPool);
				dynamicMappers = buildDynamicMappers(context, helper);
			} catch (OWLOntologyCreationException | URISyntaxException | IOException e) {
				logger.error("Could not instantiate ontology: {}", e.getMessage());
			}

			context.path().remove(); // remove name

			return new OntologyMapper(name, ontologySettings, fieldMappers, dynamicMappers, threadPool);
		}
		
		private Map<String, FieldMapper<String>> buildDynamicMappers(BuilderContext context, OntologyHelper helper) {
			Map<String, FieldMapper<String>> dynamicMappers = Maps.newHashMap();
			
			for (String relatedField : helper.getRestrictionProperties()) {
				String sanitised = relatedField.replaceAll("\\W+", "_");
				FieldMapper<String> uriMapper = MapperBuilders.stringField(sanitised + DYNAMIC_URI_FIELD_SUFFIX)
						.store(true)
						.index(true)
						.tokenized(false)
						.build(context);
				FieldMapper<String> labelMapper = MapperBuilders.stringField(sanitised + DYNAMIC_LABEL_FIELD_SUFFIX)
						.store(true)
						.index(true)
						.tokenized(true)
						.build(context);

				dynamicMappers.put(uriMapper.name(), uriMapper);
				dynamicMappers.put(labelMapper.name(), labelMapper);
				logger.debug("Add dynamic mappers for {}, {}", uriMapper.name(), labelMapper.name());
			}
			
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
		 * @param name
		 * @param node the JSON node holding the mapping definitions.
		 * @param parserContext
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
			} else if (StringUtils.isBlank(ontologySettings.getOntologyUri())) {
				throw new MapperParsingException("Ontology URI is required");
			}
			
			return new OntologyMapper.Builder(name, ontologySettings, threadPool);
		}

		private OntologySettings parseOntologySettings(Map<String, Object> ontSettingsNode) {
			OntologySettings settings = new OntologySettings();

			for (Entry<String, Object> entry : ontSettingsNode.entrySet()) {
				String key = entry.getKey();
				if (key.equals(OntologySettings.ONTOLOGY_URI_PARAM)) {
					settings.setOntologyUri(entry.getValue().toString());
				} else if (key.equals(OntologySettings.LABEL_URI_PARAM)) {
					settings.setLabelPropertyUris(extractList(entry.getValue()));
				} else if (key.equals(OntologySettings.SYNONYM_URI_PARAM)) {
					settings.setSynonymPropertyUris(extractList(entry.getValue()));
				} else if (key.equals(OntologySettings.DEFINITION_URI_PARAM)) {
					settings.setDefinitionPropertyUris(extractList(entry.getValue()));
				} else if (key.equals(OntologySettings.INCLUDE_INDIRECT_PARAM)) {
					settings.setIncludeIndirect(Boolean.parseBoolean(entry.getValue().toString()));
				} else if (key.equals(OntologySettings.INCLUDE_RELATIONS_PARAM)) {
					settings.setIncludeRelations(Boolean.parseBoolean(entry.getValue().toString()));
				}
			}

			return settings;
		}

		@SuppressWarnings("rawtypes")
		private List<String> extractList(Object value) {
			List<String> ret = null;

			if (value instanceof String) {
				ret = Arrays.asList((String) value);
			} else if (value instanceof List) {
				ret = new ArrayList<>(((List)value).size());
				for (Object v : (List)value) {
					ret.add(v.toString());
				}
			}

			return ret;
		}

	}


    private final String name;
	private final OntologySettings ontologySettings;
	private volatile ImmutableOpenMap<FieldMappings, FieldMapper<String>> fieldMappers = ImmutableOpenMap.of();
	private volatile ImmutableOpenMap<String, FieldMapper<String>> dynamicFieldMappers = ImmutableOpenMap.of();
	private final ThreadPool threadPool;
	
	private static Map<String, OntologyHelper> helpers = new ConcurrentHashMap<>();

	public OntologyMapper(String name, OntologySettings oSettings, Map<FieldMappings, FieldMapper<String>> fieldMappers, Map<String, FieldMapper<String>> dynamicMappers, ThreadPool threadPool) {
		this.name = name;
		this.ontologySettings = oSettings;
		this.fieldMappers = ImmutableOpenMap.builder(this.fieldMappers).putAll(fieldMappers).build();
		this.dynamicFieldMappers = ImmutableOpenMap.builder(this.dynamicFieldMappers).putAll(dynamicMappers).build();
		this.threadPool = threadPool;
	}


	@Override
	public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
		builder.startObject(name());
		builder.field("type", RegisterOntologyType.ONTOLOGY_TYPE);

		builder.startObject(OntologySettings.ONTOLOGY_SETTINGS_KEY);
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
            iri =  parser.text();
        } else {
        	throw new MapperParsingException(name() + " does not contain String value");
        }

        try {
			OntologyHelper helper = getHelper(ontologySettings, threadPool);

			OWLClass owlClass = helper.getOwlClass(iri);
			if (owlClass == null) {
				logger.debug("Cannot find OWL class for IRI {}", iri);
			} else {
				// We add the data as an external value, then use mapper.parse() to
				// add the field to the record. We don't need to explicitly add the
				// field - the mapper handles that.
				context.externalValue(iri);
				fieldMappers.get(FieldMappings.URI).parse(context);

				// Look up the label(s)
				Collection<String> labels = helper.findLabels(owlClass);
				for (String label : labels) {
					context.externalValue(label);
					fieldMappers.get(FieldMappings.LABEL).parse(context);
				}
				
				// Look up the synonyms
				for (String synonym : helper.findSynonyms(owlClass)) {
					context.externalValue(synonym);
					fieldMappers.get(FieldMappings.SYNONYMS).parse(context);
				}
				
				// Add the child details
				addRelatedNodesWithLabels(helper, helper.getChildUris(owlClass), context,
						fieldMappers.get(FieldMappings.CHILD_URI), fieldMappers.get(FieldMappings.CHILD_LABEL));
				
				// Add the parent details
				addRelatedNodesWithLabels(helper, helper.getParentUris(owlClass), context,
						fieldMappers.get(FieldMappings.PARENT_URI), fieldMappers.get(FieldMappings.PARENT_LABEL));
				
				if (ontologySettings.isIncludeIndirect()) {
					// Add the descendant details
					addRelatedNodesWithLabels(helper, helper.getDescendantUris(owlClass), context,
							fieldMappers.get(FieldMappings.DESCENDANT_URI), fieldMappers.get(FieldMappings.DESCENDANT_LABEL));
					
					// Add the ancestor details
					addRelatedNodesWithLabels(helper, helper.getAncestorUris(owlClass), context,
							fieldMappers.get(FieldMappings.ANCESTOR_URI), fieldMappers.get(FieldMappings.ANCESTOR_LABEL));
				}
				
				if (ontologySettings.isIncludeRelations()) {
					// Add the related nodes
					Map<String, List<String>> relations = helper.getRestrictions(owlClass);
					
					for (String relation : relations.keySet()) {
						String sanRelation = relation.replaceAll("\\W+", "_");
						String uriMapperName = sanRelation + DYNAMIC_URI_FIELD_SUFFIX;
						String labelMapperName = sanRelation + DYNAMIC_LABEL_FIELD_SUFFIX;
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
		} catch (OWLOntologyCreationException | URISyntaxException e) {
			throw new ElasticsearchException("Could not initialise ontology helper", e);
		}
	}
	
	private void addRelatedNodesWithLabels(OntologyHelper helper, Collection<String> iris, ParseContext context,
			FieldMapper<String> iriMapper, FieldMapper<String> labelMapper) throws IOException {
		if (!iris.isEmpty()) {
			for (String iri : iris) {
				context.externalValue(iri);
				iriMapper.parse(context);
			}
			for (String label : helper.findLabelsForIRIs(iris)) {
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

	
	public static OntologyHelper getHelper(OntologySettings settings, ThreadPool threadPool) throws OWLOntologyCreationException, URISyntaxException, IOException {
		OntologyHelper helper = helpers.get(settings.getOntologyUri());
		
		if (helper == null) {
			helper = new OntologyHelper(settings);
			OntologyCheckRunnable checker = new OntologyCheckRunnable(settings.getOntologyUri());
			threadPool.scheduleWithFixedDelay(checker, TimeValue.timeValueMillis(DELETE_CHECK_DELAY_MS));
			helpers.put(settings.getOntologyUri(), helper);
			helper.updateLastCallTime();
		}
		
		return helper;
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
