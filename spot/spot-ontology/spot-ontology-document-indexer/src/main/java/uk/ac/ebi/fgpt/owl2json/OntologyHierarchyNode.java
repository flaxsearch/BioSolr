package uk.ac.ebi.fgpt.owl2json;

import java.net.URI;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An object that represents a node in a directed graph representing an ontology hierarchy.  Each node should have a
 * name (usually it's label), a collection of children (formally, all nodes representing terms for which the term
 * represented by this term are related to with an is_a relationship) and a size.  How the size is determined is
 * evaluated using an {@link uk.ac.ebi.fgpt.owl2json.OntologyHierarchyNodeCounter}
 *
 * @author Tony Burdett
 * @date 01/07/13
 */
public interface OntologyHierarchyNode {
	
	@JsonProperty("uri")
    URI getURI();

	@JsonProperty("label")
    String getName();

	@JsonProperty("children")
    Collection<OntologyHierarchyNode> getChildren();

	@JsonProperty("size")
    int getSize();

    void setSize(int size);
}
