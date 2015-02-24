package org.apache.solr.xjoin;

/**
 * Interface for external process results.
 */
public interface XJoinResults {

	/**
	 * Get the external process result with given join id.
	 */
	Object getResult(String joinId);
	
	/**
	 * Get all external process join ids.
	 */
	String[] getJoinIds();

}
