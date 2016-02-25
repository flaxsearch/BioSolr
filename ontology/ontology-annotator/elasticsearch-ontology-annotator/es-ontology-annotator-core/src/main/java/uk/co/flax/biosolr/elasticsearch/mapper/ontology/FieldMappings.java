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
 * Enumeration of the fixed fields expected to be used when annotating
 * a document with ontology data. This does not include the relation
 * fields, which are generated dynamically as part of the ontology
 * mapper build process.
 *
 * @author mlp
 */
public enum FieldMappings {
	
	URI("uri"),
	LABEL("label"),
	SYNONYMS("synonyms"),
	DEFINITIONS("definitions"),
	CHILD_URI("child_uris"),
	CHILD_LABEL("child_labels"),
	PARENT_URI("parent_uris"),
	PARENT_LABEL("parent_labels"),
	DESCENDANT_URI("descendant_uris", true),
	DESCENDANT_LABEL("descendant_labels", true),
	ANCESTOR_URI("ancestor_uris", true),
	ANCESTOR_LABEL("ancestor_labels", true),
	PARENT_PATHS("parent_paths")
	;
	
	private final String fieldName;
	private final boolean indirect;
	
	FieldMappings(String field) {
		this(field, false);
	}

	FieldMappings(String field, boolean indirect) {
		this.fieldName = field;
		this.indirect = indirect;
	}

	public String getFieldName() {
		return fieldName;
	}

	public boolean isIndirect() {
		return indirect;
	}
	
	public boolean isUriField() {
		return fieldName.endsWith("uri") || fieldName.endsWith("uris");
	}
	
}
