package org.apache.solr.search.xjoin.simple;

import java.io.InputStream;

import net.minidev.json.JSONArray;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

public class JsonDocumentFactory {
  
  private Configuration configuration;
  
  public JsonDocumentFactory() {
    configuration = Configuration.builder()
                                 .options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS)
                                 .build();
  }
  
  public PathDocument read(InputStream in) {
    final DocumentContext json = JsonPath.using(configuration).parse(in);
    
    return new PathDocument() {
      
      @Override
      public Object getPathValue(String path) {
        JSONArray array = json.read(path);
        return array.size() > 0 ? array.get(0) : null;
      }

      @Override
      public Object[] getPathValues(String path) {
        return json.read(path, Object[].class);
      }
      
    };
  }

}
