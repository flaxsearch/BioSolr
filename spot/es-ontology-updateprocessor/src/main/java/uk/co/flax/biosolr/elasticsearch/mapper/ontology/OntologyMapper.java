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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.mapper.FieldMapperListener;
import org.elasticsearch.index.mapper.Mapper;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.mapper.MergeContext;
import org.elasticsearch.index.mapper.MergeMappingException;
import org.elasticsearch.index.mapper.ObjectMapperListener;
import org.elasticsearch.index.mapper.ParseContext;

/**
 * JavaDoc for OntologyMapper.
 *
 * @author mlp
 */
public class OntologyMapper implements Mapper {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.elasticsearch.common.xcontent.ToXContent#toXContent(org.elasticsearch
	 * .common.xcontent.XContentBuilder,
	 * org.elasticsearch.common.xcontent.ToXContent.Params)
	 */
	@Override
	public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.elasticsearch.index.mapper.Mapper#name()
	 */
	@Override
	public String name() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.elasticsearch.index.mapper.Mapper#parse(org.elasticsearch.index.mapper
	 * .ParseContext)
	 */
	@Override
	public void parse(ParseContext context) throws IOException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.elasticsearch.index.mapper.Mapper#merge(org.elasticsearch.index.mapper
	 * .Mapper, org.elasticsearch.index.mapper.MergeContext)
	 */
	@Override
	public void merge(Mapper mergeWith, MergeContext mergeContext) throws MergeMappingException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.elasticsearch.index.mapper.Mapper#traverse(org.elasticsearch.index
	 * .mapper.FieldMapperListener)
	 */
	@Override
	public void traverse(FieldMapperListener fieldMapperListener) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.elasticsearch.index.mapper.Mapper#traverse(org.elasticsearch.index
	 * .mapper.ObjectMapperListener)
	 */
	@Override
	public void traverse(ObjectMapperListener objectMapperListener) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.elasticsearch.index.mapper.Mapper#close()
	 */
	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	public static class TypeParser implements Mapper.TypeParser {

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
			FieldSettings fieldSettings = new FieldSettings();
			OntologySettings ontologySettings = null;
			
			for (Entry<String, Object> entry : node.entrySet()) {
				if (entry.getKey().equals(OntologySettings.ONTOLOGY_SETTINGS_KEY)) {
					ontologySettings = parseOntologySettings((Map<String, Object>) entry.getValue());
				} else if (entry.getKey().equals(FieldSettings.FIELD_SETTINGS_KEY)) {
					fieldSettings = parseFieldSettings((Map<String, Object>) entry.getValue());
				}
			}

			if (ontologySettings == null) {
				throw new MapperParsingException("No ontology settings supplied");
			} else if (StringUtils.isBlank(ontologySettings.getOntologyUri())) {
				throw new MapperParsingException("Ontology URI is required");
			}

			return new OntologyMapper.Builder(name, ontologySettings, fieldSettings);
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
		
		private FieldSettings parseFieldSettings(Map<String, Object> fieldSettingsNode) {
			FieldSettings settings = new FieldSettings();
			
			for (Entry<String, Object> entry : fieldSettingsNode.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue().toString();
				
				if (key.equals(FieldSettings.LABEL_FIELD_PARAM)) {
					settings.setLabelField(value);
				} else if (key.equals(FieldSettings.URI_FIELD_SUFFIX_PARAM)) {
					settings.setUriFieldSuffix(value);
				} else if (key.equals(FieldSettings.LABEL_FIELD_SUFFIX_PARAM)) {
					settings.setLabelFieldSuffix(value);
				} else if (key.equals(FieldSettings.CHILD_FIELD_PARAM)) {
					settings.setChildField(value);
				} else if (key.equalsIgnoreCase(FieldSettings.PARENT_FIELD_PARAM)) {
					settings.setParentField(value);
				} else if (key.equals(FieldSettings.DESCENDANT_FIELD_PARAM)) {
					settings.setDescendantField(value);
				} else if (key.equals(FieldSettings.ANCESTOR_FIELD_PARAM)) {
					settings.setAncestorField(value);
				} else if (key.equals(FieldSettings.SYNONYMS_FIELD_PARAM)) {
					settings.setSynonymsField(value);
				} else if (key.equals(FieldSettings.DEFINITION_FIELD_PARAM)) {
					settings.setDefinitionField(value);
				} else if (key.equals(FieldSettings.INCLUDE_INDIRECT_PARAM)) {
					settings.setIncludeIndirect(Boolean.valueOf(value));
				} else if (key.equals(FieldSettings.INCLUDE_RELATIONS_PARAM)) {
					settings.setIncludeRelations(Boolean.valueOf(value));
				}
			}
			
			return settings;
		}

	}

	public static class Builder extends Mapper.Builder<Builder, OntologyMapper> {
		
		private final OntologySettings ontologySettings;
		private final FieldSettings fieldSettings;

		public Builder(String name, OntologySettings ontSettings, FieldSettings fieldSettings) {
			super(name);
			this.ontologySettings = ontSettings;
			this.fieldSettings = fieldSettings;
		}

		@Override
		public OntologyMapper build(BuilderContext context) {
			// TODO Auto-generated method stub
			return null;
		}

	}

}
