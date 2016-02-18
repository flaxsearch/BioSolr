package uk.co.flax.biosolr.ontology.loaders;

/**
 * @author Simon Jupp
 * @date 03/02/2015 Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class OntologyLoadingException extends Exception {

	private static final long serialVersionUID = 1L;

	public OntologyLoadingException() {
	}

	public OntologyLoadingException(String message) {
		super(message);
	}

	public OntologyLoadingException(String message, Throwable cause) {
		super(message, cause);
	}

	public OntologyLoadingException(Throwable cause) {
		super(cause);
	}

	public OntologyLoadingException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
