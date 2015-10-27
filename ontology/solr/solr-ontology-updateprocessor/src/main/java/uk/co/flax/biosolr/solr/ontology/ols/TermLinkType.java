package uk.co.flax.biosolr.solr.ontology.ols;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enumeration representing the types of links returned in
 * {@link OntologyTerm} objects.
 *
 * Created by mlp on 27/10/15.
 *
 * @author mlp
 */
public enum TermLinkType {

	SELF,
	PARENTS,
	ANCESTORS,
	CHILDREN,
	DESCENDANTS,
	JSTREE,
	GRAPH,
	UNKNOWN
	;

	@JsonCreator
	public static TermLinkType fromValue(String value) {
		// Default to UNKNOWN - catch-all for undocumented values
		TermLinkType ret = TermLinkType.UNKNOWN;

		for (TermLinkType lt : TermLinkType.values()) {
			if (lt.name().equalsIgnoreCase(value)) {
				ret = lt;
				break;
			}
		}

		return ret;
	}

	public String toString() {
		return name().toLowerCase();
	}

}
