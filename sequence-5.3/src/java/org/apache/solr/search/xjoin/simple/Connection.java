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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

import org.apache.solr.common.params.SolrParams;

public class Connection implements AutoCloseable {
  
  private URLConnection cnx;
  
  private InputStream in;
  
  private String url;
  
  public Connection(String rootUrl, String accept, SolrParams params) throws IOException {
    StringBuilder sb = new StringBuilder("?");
    for (Iterator<String> it = params.getParameterNamesIterator(); it.hasNext(); ) {
      String name = it.next();
      for (String value : params.getParams(name)) {
        sb.append(name);
        sb.append("=");
        sb.append(value);
        sb.append("&");
      }
    }
    
    url = rootUrl;
    if (sb.length() > 1) {
      url += sb.substring(0, sb.length() - 1);
    }

    cnx = new URL(url).openConnection();
    cnx.setRequestProperty("Accept", accept);
    in = null;
  }
  
  public void open() throws IOException {
    if (in == null) {
      in = cnx.getInputStream();
    }
  }
  
  public String getUrl() {
    return url;
  }
  
  public InputStream getInputStream() throws IOException {
    return in;
  }
  
  @Override
  public void close() {
    try {
      if (in != null) {
        in.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
}
