package uk.co.flax.biosolr.ontology.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * 
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
	
	@Field("child_hierarchy")
	private String childHierarchy;
	
	@Field("tree_level")
	private int treeLevel;
	
	@Field("*_rel")
	private Map<String, List<String>> relations = new HashMap<>();
	
    public EFOAnnotation() {
    }

    public EFOAnnotation(String uri, String shortFrom, List<String> label, List<String> synonym, List<String> description) {
        this.uri = uri;
        this.shortForm = shortFrom;
        this.label = label;
        this.synonym = synonym;
        this.description = description;
    }

    public EFOAnnotation(String uri, String shortFrom, Set<String> label, Set<String> synonym, Set<String> description) {
        this.uri = uri;
        this.shortForm = shortFrom;
        this.label = new ArrayList<String>(label);
        this.synonym = new ArrayList<String>(synonym);
        this.description = new ArrayList<String>(description);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getShortForm() {
        return shortForm;
    }

    public void setShortForm(String shortFrom) {
        this.shortForm = shortFrom;
    }

    public List<String> getLabel() {
        return label;
    }

    public void setLabel(List<String> label) {
        this.label = label;
    }

    public List<String> getSynonym() {
        return synonym;
    }

    public void setSynonym(List<String> synonym) {
        this.synonym = synonym;
    }
    
    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "EFOAnnotation{" +
                "uri='" + uri + '\'' +
                ", shortFrom='" + shortForm + '\'' +
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

	/**
	 * @return the superclassUris
	 */
	public List<String> getAncestorUris() {
		return ancestorUris;
	}

	/**
	 * @param superclassUris the superclassUris to set
	 */
	public void setAncestorUris(List<String> superclassUris) {
		this.ancestorUris = superclassUris;
	}

	/**
	 * @return the subclassUris
	 */
	public List<String> getChildUris() {
		return childUris;
	}

	/**
	 * @param subclassUris the subclassUris to set
	 */
	public void setChildUris(List<String> subclassUris) {
		this.childUris = subclassUris;
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
	 * @return the childHierarchy
	 */
	public String getChildHierarchy() {
		return childHierarchy;
	}

	/**
	 * @param childHierarchy the childHierarchy to set
	 */
	public void setChildHierarchy(String childHierarchy) {
		this.childHierarchy = childHierarchy;
	}

	/**
	 * @return the treeLevel
	 */
	public int getTreeLevel() {
		return treeLevel;
	}

	/**
	 * @param treeLevel the treeLevel to set
	 */
	public void setTreeLevel(int treeLevel) {
		this.treeLevel = treeLevel;
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

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ancestorUris == null) ? 0 : ancestorUris.hashCode());
		result = prime * result + ((childHierarchy == null) ? 0 : childHierarchy.hashCode());
		result = prime * result + ((childUris == null) ? 0 : childUris.hashCode());
		result = prime * result + ((descendentUris == null) ? 0 : descendentUris.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + idKey;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((parentUris == null) ? 0 : parentUris.hashCode());
		result = prime * result + ((relations == null) ? 0 : relations.hashCode());
		result = prime * result + ((shortForm == null) ? 0 : shortForm.hashCode());
		result = prime * result + ((synonym == null) ? 0 : synonym.hashCode());
		result = prime * result + treeLevel;
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EFOAnnotation other = (EFOAnnotation) obj;
		if (ancestorUris == null) {
			if (other.ancestorUris != null)
				return false;
		} else if (!ancestorUris.equals(other.ancestorUris))
			return false;
		if (childHierarchy == null) {
			if (other.childHierarchy != null)
				return false;
		} else if (!childHierarchy.equals(other.childHierarchy))
			return false;
		if (childUris == null) {
			if (other.childUris != null)
				return false;
		} else if (!childUris.equals(other.childUris))
			return false;
		if (descendentUris == null) {
			if (other.descendentUris != null)
				return false;
		} else if (!descendentUris.equals(other.descendentUris))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (idKey != other.idKey)
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (parentUris == null) {
			if (other.parentUris != null)
				return false;
		} else if (!parentUris.equals(other.parentUris))
			return false;
		if (relations == null) {
			if (other.relations != null)
				return false;
		} else if (!relations.equals(other.relations))
			return false;
		if (shortForm == null) {
			if (other.shortForm != null)
				return false;
		} else if (!shortForm.equals(other.shortForm))
			return false;
		if (synonym == null) {
			if (other.synonym != null)
				return false;
		} else if (!synonym.equals(other.synonym))
			return false;
		if (treeLevel != other.treeLevel)
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

}
