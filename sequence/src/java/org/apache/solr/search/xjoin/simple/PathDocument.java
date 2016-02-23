package org.apache.solr.search.xjoin.simple;

public interface PathDocument {
  
  public Object getPathValue(String path);
  
  public Object[] getPathValues(String path);
  
}
