package uk.co.flax.biosolr.ontology.api;

import java.util.Set;

import org.apache.solr.client.solrj.beans.Field;

/**
 * Copyright 2014 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * /
 * <p/>
 * <p/>
 * /**
 *
 * @author Simon Jupp
 * @date 23/10/2012
 * Functional Genomics Group EMBL-EBI
 */
public class EFOAnnotation {

	@Field("id")
	private String id;
	
	@Field("uri_key")
	private int idKey;
	
	@Field("uri")
    private String uri;
	
	@Field("short_form")
    private String shortFrom;
	
	@Field("label")
    private Set<String> label;
	
	@Field("synonyms")
    private Set<String> synonym;
	
	@Field("description")
    private Set<String> description;

    public EFOAnnotation() {
    }

    public EFOAnnotation(String uri, String shortFrom, Set<String> label, Set<String> synonym, Set<String> description) {
        this.uri = uri;
        this.shortFrom = shortFrom;
        this.label = label;
        this.synonym = synonym;
        this.description = description;
    }

    public String getUri() {

        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getShortFrom() {
        return shortFrom;
    }

    public void setShortFrom(String shortFrom) {
        this.shortFrom = shortFrom;
    }

    public Set<String> getLabel() {
        return label;
    }

    public void setLabel(Set<String> label) {
        this.label = label;
    }

    public Set<String> getSynonym() {
        return synonym;
    }

    public void setSynonym(Set<String> synonym) {
        this.synonym = synonym;
    }

    public Set<String> getDescription() {
        return description;
    }

    public void setDescription(Set<String> description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "EFOAnnotation{" +
                "uri='" + uri + '\'' +
                ", shortFrom='" + shortFrom + '\'' +
                ", label=" + label +
                ", synonym=" + synonym +
                ", description=" + description +
                '}';
    }

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
}
