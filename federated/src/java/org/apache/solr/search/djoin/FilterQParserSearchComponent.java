package org.apache.solr.search.djoin;

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
import java.util.Set;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SyntaxError;

public class FilterQParserSearchComponent extends SearchComponent {

  // initialisation parameters
  public static final String INIT_QPARSER = "qParser";
  
  private String qParser;

  @Override
  @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
    super.init(args);
  
    qParser = (String)args.get(INIT_QPARSER);
  }

  @Override
  public void prepare(ResponseBuilder rb) throws IOException {
    // do nothing
  }

  @Override
  public void process(ResponseBuilder rb) throws IOException {
    // do nothing
  }
  
  @Override
  public void modifyRequest(ResponseBuilder rb, SearchComponent who, ShardRequest sreq) {
    Set<String> names = sreq.params.getParameterNames();
    for (String name : names.toArray(new String[names.size()])) {
      for (String value : sreq.params.getParams(name)) {
        try {
          SolrParams params = QueryParsing.getLocalParams(value, sreq.params);
          if (params != null && params.get("type").equals(qParser)) {
            sreq.params.remove(name, value);
          }
        } catch (SyntaxError e) {
          // ignore
        }
      }
    }
  }
  
  @Override
  public String getDescription() {
    return "$description";
  }

  @Override
  public String getSource() {
    return "$source";
  }

}
