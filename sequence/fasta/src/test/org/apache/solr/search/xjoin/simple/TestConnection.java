package org.apache.solr.search.xjoin.simple;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
