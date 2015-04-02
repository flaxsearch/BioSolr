package org.apache.solr.search.xjoin;

/**
 * Interface for external process results.
 */
public interface XJoinResults<IdType> {

	/**
	 * Get the external process result with given join id.
	 */
	Object getResult(IdType joinId);
	
	/**
	 * Get an ordered (ascending) iterable of external process join ids (null is
	 * not a valid id).
	 */
	Iterable<IdType> getOrderedJoinIds();

}
