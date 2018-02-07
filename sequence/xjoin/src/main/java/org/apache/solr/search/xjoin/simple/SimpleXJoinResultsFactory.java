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
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.xjoin.XJoinResults;
import org.apache.solr.search.xjoin.XJoinResultsFactory;
import org.xml.sax.SAXException;

public class SimpleXJoinResultsFactory implements XJoinResultsFactory<String> {

  public static final String INIT_PARAM_TYPE = "type";
  public static final String INIT_PARAM_ROOT_URL = "rootUrl";
  public static final String INIT_PARAM_GLOBAL_FIELD_PATHS = "globalFieldPaths";
  public static final String INIT_PARAM_JOIN_ID_PATH = "joinIdPath";
  public static final String INIT_PARAM_JOIN_ID_TOKEN = "joinIdToken";
  public static final String INIT_PARAM_RESULT_FIELD_PATHS = "resultFieldPaths";
  
  public static final String DEFAULT_JOIN_ID_TOKEN = "JOINID";
  
  public static enum Type {
    JSON {
      private final JsonDocumentFactory documentFactory = new JsonDocumentFactory();
      
      @Override
      protected String getMimeType() {
        return "application/json";
      }

      @Override
      protected PathDocument read(InputStream in) {
        return documentFactory.read(in);
      }
    },
    
    XML {
      private final XmlDocumentFactory documentFactory = new XmlDocumentFactory();
      
      @Override
      protected String getMimeType() {
        return "application/xml";
      }

      @Override
      protected PathDocument read(InputStream in) throws IOException {
        try {
          return documentFactory.read(in);
        } catch (SAXException e) {
          throw new IOException(e);
        }
      }
    };
    
    protected abstract String getMimeType();
    protected abstract PathDocument read(InputStream in) throws IOException;
  }
  
  private Type type;
  
  private String rootUrl;
  
  private Map<String, String> globalFieldPaths;
  
  private String joinIdPath;
  
  private String joinIdToken;
  
  private Map<String, String> resultFieldPaths;
  
  /**
   * 
   */
  @Override
  @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
    type = Type.valueOf((String)args.get(INIT_PARAM_TYPE));
    rootUrl = (String)args.get(INIT_PARAM_ROOT_URL);
    
    globalFieldPaths = new HashMap<>();
    NamedList globals = (NamedList)args.get(INIT_PARAM_GLOBAL_FIELD_PATHS);
    if (globals != null) {
      for (int i = 0; i < globals.size(); ++i) {
        String fieldName = globals.getName(i);
        String value = (String)globals.getVal(i);
        globalFieldPaths.put(fieldName, value);
      }
    }
    
    joinIdPath = (String)args.get(INIT_PARAM_JOIN_ID_PATH);
 
    joinIdToken = (String)args.get(INIT_PARAM_JOIN_ID_TOKEN);
    if (joinIdToken == null) {
      joinIdToken = DEFAULT_JOIN_ID_TOKEN;
    }
    
    resultFieldPaths = new HashMap<>();
    NamedList results = (NamedList)args.get(INIT_PARAM_RESULT_FIELD_PATHS);
    if (results != null) {
      for (int i = 0; i < results.size(); ++i) {
        String fieldName = results.getName(i);
        String value = (String)results.getVal(i);
        resultFieldPaths.put(fieldName, value);
      }
    }
  }

  @Override
  public XJoinResults<String> getResults(SolrParams params) throws IOException {
    try (Connection cnx = new Connection(rootUrl, type.getMimeType(), params)) {
      cnx.open();
      return new Results(type.read(cnx.getInputStream()));
    }
  }
  
  @SuppressWarnings("serial")
  public class Results extends HashMap<String, Object> implements XJoinResults<String> {
    
    private Map<String, Map<String, Object>> results;
    
    private Results(PathDocument doc) {
      for (String fieldName : globalFieldPaths.keySet()) {
        put(fieldName, doc.getPathValue(globalFieldPaths.get(fieldName)));
      }
      
      results = new HashMap<>();
      for (Object joinId : doc.getPathValues(joinIdPath)) {
        Map<String, Object> result = new HashMap<>();
        results.put(joinId.toString(), result);
        for (String fieldName : resultFieldPaths.keySet()) {
          String path = resultFieldPaths.get(fieldName);
          path = path.replace(joinIdToken, joinId.toString());
          Object value = doc.getPathValue(path);
          if (value != null) {
            result.put(fieldName, value);
          }
        }
      }
    }
    
    @Override
    public Object getResult(String joinIdStr) {
      return results.get(joinIdStr);
    }

    @Override
    public Iterable<String> getJoinIds() {
      return results.keySet();
    }
    
  }

}
