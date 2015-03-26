package org.apache.solr.search.xjoin;

import org.apache.solr.common.params.CommonParams;

/**
 * Parameters for XJoin functionality.
 */
public class XJoinParameters {

	// XJoinSearchComponent parameters
	public static final String INIT_RESULTS_FACTORY = "factoryClass";
	public static final String INIT_JOIN_FIELD = "joinField";
	public static final String EXTERNAL_PREFIX = "external";
	public static final String LIST_PARAMETER = "listParameter";
	public static final String RESULTS_FIELD_LIST = "results";
	public static final String DOC_FIELD_LIST = CommonParams.FL;

	// XJoinValueSourceParser parameters
	public static final String INIT_XJOIN_COMPONENT_NAME = "xJoinSearchComponent";
	public static final String INIT_ATTRIBUTE = "attribute";
	public static final String INIT_DEFAULT_VALUE = "defaultValue";
	
	// XJoinQParserPlugin parameters
	public static final String INIT_FIELD = "f";
	
}
