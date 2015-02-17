package uk.ac.ebi.fgpt.owl2json;

/**
 * A mechanism for attributing sizes to {@link uk.ac.ebi.fgpt.owl2json.OntologyHierarchyNode}s.  There are several
 * possible implementations of this class, but generally these will be separated into those that consider the ontology
 * itself (and therefore attribute size based, for example, on the total number of child terms a given term has) or
 * those that consider some data resource (and therefore attribute size based on the number of data points that are
 * annotated to a given term, for example)
 *
 * @author Tony Burdett
 * @date 18/08/14
 */
public interface OntologyHierarchyNodeCounter {
    /**
     * Evaluates and returns the size for a given ontology hierarchy node
     *
     * @param node the node to count
     * @return the size that should be attributed to this node
     */
    int count(OntologyHierarchyNode node);
}
