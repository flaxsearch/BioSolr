package org.apache.solr.search.xjoin;

/**
 * Interface for external process results.
 */
public interface XJoinResults<IdType> {
  
  /**
   * Get the external process result with given join id string.
   * 
   * Note: you might need to convert the argument to the correct type.
   */
  Object getResult(String joinIdStr);
  
  /**
   * Get an ordered (ascending) iterable of external process join ids (null is
   * not a valid id).
   */
  Iterable<IdType> getJoinIds();

}
