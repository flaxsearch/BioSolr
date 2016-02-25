package uk.co.flax.biosolr.ontology.core.ols.terms;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enumeration representing the basic types of links returned in
 * {@link OntologyTerm} objects. The links returned from OLS may
 * include additional types, depending on the relations stored
 * for the term.
 *
 * <p>Created by Matt Pearce on 27/10/15.</p>
 *
 * @author Matt Pearce
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
