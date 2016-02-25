/**
 * The base package for the Ontology Annotator core classes.
 *
 * <p>
 * The {@link uk.co.flax.biosolr.ontology.core.OntologyHelper} interface
 * defines the functionality for retrieving data from an ontology. There
 * are OWL and OLS implementations of this interface for working with
 * OWL files and the EBI OLS web API respectively.
 * </p>
 *
 * <p>
 * The {@link uk.co.flax.biosolr.ontology.core.OntologyData} class
 * holds the data that has been retrieved from the ontology. The
 * {@link uk.co.flax.biosolr.ontology.core.OntologyDataBuilder} should
 * be used in conjunction with an {@code OntologyHelper} to build the
 * OntologyData.
 * </p>
 *
 * @author Matt Pearce
 */
package uk.co.flax.biosolr.ontology.core;