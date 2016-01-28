package uk.co.flax.biosolr.ontology.core;

/**
 * Interface defining the OntologyHelper factory class.
 *
 * <p>
 * An ElasticOntologyHelperFactory should be instantiated by
 * passing in whatever parameters are required to indicate
 * the type of OntologyHelper required.
 * </p>
 *
 * Created by mlp on 03/11/15.
 *
 * @author mlp
 */
public interface OntologyHelperFactory {

	/**
	 * Build an {@link OntologyHelper} instance suitable for the search engine
	 * being used.
	 * @return the OntologyHelper.
	 * @throws OntologyHelperException if problems occur constructing the helper
	 * - eg. the OWL file cannot be found, or similar.
	 */
	OntologyHelper buildOntologyHelper() throws OntologyHelperException;

}
