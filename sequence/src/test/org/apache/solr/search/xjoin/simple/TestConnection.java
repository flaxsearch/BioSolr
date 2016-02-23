package org.apache.solr.search.xjoin.simple;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.Test;

public class TestConnection {

  private final static String ROOT_URL = "http://example.com/endpoint";

  @Test
  public void testUrl() throws IOException {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.add("foo", "bar");
    params.add("foo", "boz");
    params.add("boo", "baz");
    try (Connection cnx = new Connection(ROOT_URL, "", params)) {
      assertEquals(ROOT_URL + "?foo=bar&foo=boz&boo=baz", cnx.getUrl());
    }
  }
  
  @Test
  public void testNoParams() throws IOException {
    ModifiableSolrParams params = new ModifiableSolrParams();
    try (Connection cnx = new Connection(ROOT_URL, "", params)) {
      assertEquals(ROOT_URL, cnx.getUrl());
    }
  }
  
}
