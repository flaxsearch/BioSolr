package uk.co.flax.biosolr.ontology.core.ols.terms;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enumeration representing the types of links that may be returned in an
 * OLS query result (ie. {@link RelatedTermsResult}).
 *
 * <p>Created by Matt Pearce on 21/10/15.</p>
 *
 * @author Matt Pearce
 */
public enum ResultsLinkType {

	SELF,
	FIRST,
	LAST,
	NEXT,
	PREV
	;

	@JsonCreator
	public static ResultsLinkType fromValue(String value) {
		ResultsLinkType ret = null;

		for (ResultsLinkType lt : ResultsLinkType.values()) {
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
