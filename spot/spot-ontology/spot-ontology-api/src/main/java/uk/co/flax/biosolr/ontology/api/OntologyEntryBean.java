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
package uk.co.flax.biosolr.ontology.api;

import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.beans.Field;

/**
 * A bean representing a single ontology entry, as stored in the data store.
 * 
 * @author Matt Pearce
 */
public class OntologyEntryBean {

	@Field("id")
	private String id;
	
	@Field("uri_key")
	private int idKey;
	
	@Field("uri")
    private String uri;
	
	@Field("source")
	private String source;
	
	@Field("short_form")
    private String shortForm;
	
	@Field("label")
    private List<String> label;
	
	@Field("synonyms")
    private List<String> synonym;
	
	@Field("description")
    private List<String> description;
	
	@Field("parent_uris")
	private List<String> parentUris;
	
	@Field("ancestor_uris")
	private List<String> ancestorUris;
	
	@Field("child_uris")
	private List<String> childUris;
	
	@Field("descendent_uris")
	private List<String> descendentUris;
	
	@Field("*_rel")
	private Map<String, List<String>> relations;
	
	@Field("*")
	private Map<String, Object> additionalFields;

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the idKey
	 */
	public int getIdKey() {
		return idKey;
	}

	/**
	 * @param idKey the idKey to set
	 */
	public void setIdKey(int idKey) {
		this.idKey = idKey;
	}

	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * @param uri the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * @return the shortForm
	 */
	public String getShortForm() {
		return shortForm;
	}

	/**
	 * @param shortForm the shortForm to set
	 */
	public void setShortForm(String shortForm) {
		this.shortForm = shortForm;
	}

	/**
	 * @return the label
	 */
	public List<String> getLabel() {
		return label;
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(List<String> label) {
		this.label = label;
	}

	/**
	 * @return the synonym
	 */
	public List<String> getSynonym() {
		return synonym;
	}

	/**
	 * @param synonym the synonym to set
	 */
	public void setSynonym(List<String> synonym) {
		this.synonym = synonym;
	}

	/**
	 * @return the description
	 */
	public List<String> getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(List<String> description) {
		this.description = description;
	}

	/**
	 * @return the parentUris
	 */
	public List<String> getParentUris() {
		return parentUris;
	}

	/**
	 * @param parentUris the parentUris to set
	 */
	public void setParentUris(List<String> parentUris) {
		this.parentUris = parentUris;
	}

	/**
	 * @return the ancestorUris
	 */
	public List<String> getAncestorUris() {
		return ancestorUris;
	}

	/**
	 * @param ancestorUris the ancestorUris to set
	 */
	public void setAncestorUris(List<String> ancestorUris) {
		this.ancestorUris = ancestorUris;
	}

	/**
	 * @return the childUris
	 */
	public List<String> getChildUris() {
		return childUris;
	}

	/**
	 * @param childUris the childUris to set
	 */
	public void setChildUris(List<String> childUris) {
		this.childUris = childUris;
	}

	/**
	 * @return the descendentUris
	 */
	public List<String> getDescendentUris() {
		return descendentUris;
	}

	/**
	 * @param descendentUris the descendentUris to set
	 */
	public void setDescendentUris(List<String> descendentUris) {
		this.descendentUris = descendentUris;
	}

	/**
	 * @return the relations
	 */
	public Map<String, List<String>> getRelations() {
		return relations;
	}

	/**
	 * @param relations the relations to set
	 */
	public void setRelations(Map<String, List<String>> relations) {
		this.relations = relations;
	}

	/**
	 * @return the additionalFields
	 */
	public Map<String, Object> getAdditionalFields() {
		return additionalFields;
	}

	/**
	 * @param additionalFields the additionalFields to set
	 */
	public void setAdditionalFields(Map<String, Object> additionalFields) {
		this.additionalFields = additionalFields;
	}
	
}
