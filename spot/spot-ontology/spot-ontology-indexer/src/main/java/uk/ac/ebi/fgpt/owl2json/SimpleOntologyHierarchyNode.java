package uk.ac.ebi.fgpt.owl2json;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

/**
 * A node in the ontology hierarchy that represents a single ontology class.  The name of this node would normally be
 * the rdfs:label.
 *
 * @author Tony Burdett
 * @date 01/07/13
 */
public class SimpleOntologyHierarchyNode implements OntologyHierarchyNode {
    private final URI uri;
    private final String name;
    private final Collection<OntologyHierarchyNode> children;
    private int size;

    public SimpleOntologyHierarchyNode() {
        this("");
    }

    public SimpleOntologyHierarchyNode(String name) {
        this(name, Collections.<OntologyHierarchyNode>emptySet());
    }

    public SimpleOntologyHierarchyNode(String name, Collection<OntologyHierarchyNode> children) {
        this(null, name, children);
    }

    public SimpleOntologyHierarchyNode(URI uri, String name, Collection<OntologyHierarchyNode> children) {
        this.uri = uri;
        this.name = name;
        this.children = children;
        this.size = -1;
    }

    @Override public URI getURI() {
        return uri;
    }

    @Override public String getName() {
        return name;
    }

    @Override public Collection<OntologyHierarchyNode> getChildren() {
        return children;
    }

    @Override public int getSize() {
        return size;
    }

    @Override public void setSize(int size) {
        this.size = size;
    }
}
