package org.apache.solr.search.xjoin;

import java.io.IOException;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

/**
 * Interface for external process results factories.
 */
public interface XJoinResultsFactory<IdType> {
  
  /**
   * Initialise the factory with the given parameters.
   */
  @SuppressWarnings("rawtypes")
  public void init(NamedList args);

  /**
   * Get external process results based on the given parameters.
   */
  public XJoinResults<IdType> getResults(SolrParams params) throws IOException;

}
