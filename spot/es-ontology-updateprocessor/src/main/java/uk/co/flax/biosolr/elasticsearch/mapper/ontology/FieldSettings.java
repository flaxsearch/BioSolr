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



/**
 * JavaDoc for OntologyMappingSettings.
 *
 * @author mlp
 */
public class FieldSettings {
	
	static final String FIELD_SETTINGS_KEY = "fields";
	
	/*
	 * Field configuration parameters
	 */
	static final String LABEL_FIELD_PARAM = "labelField";
	static final String URI_FIELD_SUFFIX_PARAM = "uriFieldSuffix";
	static final String LABEL_FIELD_SUFFIX_PARAM = "labelFieldSuffix";
	static final String CHILD_FIELD_PARAM = "childField";
	static final String PARENT_FIELD_PARAM = "parentField";
	static final String INCLUDE_INDIRECT_PARAM = "includeIndirect";
	static final String DESCENDANT_FIELD_PARAM = "descendantsField";
	static final String ANCESTOR_FIELD_PARAM = "ancestorsField";
	static final String INCLUDE_RELATIONS_PARAM = "includeRelations";
	static final String SYNONYMS_FIELD_PARAM = "synonymsField";
	static final String DEFINITION_FIELD_PARAM = "definitionField";
	
	/*
	 * Default field values
	 */
	private static final String LABEL_FIELD_DEFAULT = "label_t";
	private static final String URI_FIELD_SUFFIX = "_uris_s";
	private static final String LABEL_FIELD_SUFFIX = "_labels_t";
	private static final String CHILD_FIELD_DEFAULT = "child";
	private static final String PARENT_FIELD_DEFAULT = "parent";
	private static final String DESCENDANT_FIELD_DEFAULT = "descendants";
	private static final String ANCESTOR_FIELD_DEFAULT = "ancestors";
	private static final String SYNONYMS_FIELD_DEFAULT = "synonyms_t";
	private static final String DEFINITION_FIELD_DEFAULT = "definition_t";
	
	private String labelField = LABEL_FIELD_DEFAULT;
	private String uriFieldSuffix = URI_FIELD_SUFFIX;
	private String labelFieldSuffix = LABEL_FIELD_SUFFIX;
	private String childField;
	private String parentField;
	private String descendantField;
	private String ancestorField;
	private String childUriField;
	private String childLabelField;
	private String parentUriField;
	private String parentLabelField;
	private boolean includeIndirect = true;
	private String descendantUriField;
	private String descendantLabelField;
	private String ancestorUriField;
	private String ancestorLabelField;
	private boolean includeRelations = true;
	private String synonymsField = SYNONYMS_FIELD_DEFAULT;
	private String definitionField = DEFINITION_FIELD_DEFAULT;
	
	public String getLabelField() {
		if (labelField == null) {
			return LABEL_FIELD_DEFAULT;
		}
		return labelField;
	}
	
	public String getChildUriField() {
		if (childUriField == null) {
			childUriField = (childField == null ? CHILD_FIELD_DEFAULT : childField) + (uriFieldSuffix == null ? URI_FIELD_SUFFIX : uriFieldSuffix);
		}
		return childUriField;
	}

	public String getChildLabelField() {
		if (childLabelField == null) {
			childLabelField = (childField == null ? CHILD_FIELD_DEFAULT : childField) + (labelFieldSuffix == null ? URI_FIELD_SUFFIX : uriFieldSuffix);
		}
		return childLabelField;
	}

	public String getParentUriField() {
		if (parentUriField == null) {
			parentUriField = (parentField == null ? PARENT_FIELD_DEFAULT : parentField) + (uriFieldSuffix == null ? URI_FIELD_SUFFIX : uriFieldSuffix);
		}
		return parentUriField;
	}

	public String getParentLabelField() {
		if (parentLabelField == null) {
			parentLabelField = (parentField == null ? PARENT_FIELD_DEFAULT : parentField) + (labelFieldSuffix == null ? URI_FIELD_SUFFIX : uriFieldSuffix);
		}
		return parentLabelField;
	}
	
	public boolean isIncludeIndirect() {
		return includeIndirect;
	}
	
	public String getDescendantUriField() {
		if (descendantUriField == null) {
			descendantUriField = (descendantField == null ? DESCENDANT_FIELD_DEFAULT : descendantField) + (uriFieldSuffix == null ? URI_FIELD_SUFFIX : uriFieldSuffix);
		}
		return descendantUriField;
	}
	
	public String getDescendantLabelField() {
		if (descendantLabelField == null) {
			descendantLabelField = (descendantField == null ? DESCENDANT_FIELD_DEFAULT : descendantField) + (labelFieldSuffix == null ? URI_FIELD_SUFFIX : uriFieldSuffix);
		}
		return descendantLabelField; 
	}
	
	public String getAncestorUriField() {
		if (ancestorUriField == null) {
			ancestorUriField = (ancestorField == null ? ANCESTOR_FIELD_DEFAULT : ancestorField) + (uriFieldSuffix == null ? URI_FIELD_SUFFIX : uriFieldSuffix);
		}
		return ancestorUriField;
	}
	
	public String getAncestorLabelField() {
		if (ancestorLabelField == null) {
			ancestorLabelField = (ancestorField == null ? ANCESTOR_FIELD_DEFAULT : ancestorField) + (labelFieldSuffix == null ? URI_FIELD_SUFFIX : uriFieldSuffix);
		}
		return ancestorLabelField;
	}
	
	public boolean isIncludeRelations() {
		return includeRelations;
	}
	
	public String getUriFieldSuffix() {
		return uriFieldSuffix;
	}
	
	public String getLabelFieldSuffix() {
		return labelFieldSuffix;
	}
	
	public String getSynonymsField() {
		return synonymsField;
	}

	public String getDefinitionField() {
		return definitionField;
	}

	public void setLabelField(String labelField) {
		this.labelField = labelField;
	}

	public void setUriFieldSuffix(String uriFieldSuffix) {
		this.uriFieldSuffix = uriFieldSuffix;
	}

	public void setLabelFieldSuffix(String labelFieldSuffix) {
		this.labelFieldSuffix = labelFieldSuffix;
	}

	public void setChildField(String childField) {
		this.childField = childField;
	}

	public void setParentField(String parentField) {
		this.parentField = parentField;
	}

	public void setDescendantField(String descendantField) {
		this.descendantField = descendantField;
	}

	public void setAncestorField(String ancestorField) {
		this.ancestorField = ancestorField;
	}

	public void setChildUriField(String childUriField) {
		this.childUriField = childUriField;
	}

	public void setChildLabelField(String childLabelField) {
		this.childLabelField = childLabelField;
	}

	public void setParentUriField(String parentUriField) {
		this.parentUriField = parentUriField;
	}

	public void setParentLabelField(String parentLabelField) {
		this.parentLabelField = parentLabelField;
	}

	public void setIncludeIndirect(boolean includeIndirect) {
		this.includeIndirect = includeIndirect;
	}

	public void setDescendantUriField(String descendantUriField) {
		this.descendantUriField = descendantUriField;
	}

	public void setDescendantLabelField(String descendantLabelField) {
		this.descendantLabelField = descendantLabelField;
	}

	public void setAncestorUriField(String ancestorUriField) {
		this.ancestorUriField = ancestorUriField;
	}

	public void setAncestorLabelField(String ancestorLabelField) {
		this.ancestorLabelField = ancestorLabelField;
	}

	public void setIncludeRelations(boolean includeRelations) {
		this.includeRelations = includeRelations;
	}

	public void setSynonymsField(String synonymsField) {
		this.synonymsField = synonymsField;
	}

	public void setDefinitionField(String definitionField) {
		this.definitionField = definitionField;
	}

}
