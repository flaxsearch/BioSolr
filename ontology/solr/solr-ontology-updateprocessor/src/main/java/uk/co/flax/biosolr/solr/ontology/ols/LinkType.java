package uk.co.flax.biosolr.solr.ontology.ols;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enumeration representing the types of links that may be returned in an OLS
 * OntologyTerms object.
 *
 * Created by mlp on 21/10/15.
 *
 * @author mlp
 */
public enum LinkType {

	SELF,
	PARENTS,
	ANCESTORS,
	CHILDREN,
	DESCENDANTS,
	JSTREE,
	GRAPH
	;

	@JsonCreator
	public static LinkType fromValue(String value) {
		LinkType ret = null;

		for (LinkType lt : LinkType.values()) {
			if (lt.name().equalsIgnoreCase(value)) {
				ret = lt;
				break;
			}
		}

		return ret;
	}

}
