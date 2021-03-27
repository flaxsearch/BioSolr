package org.apache.solr.search.xjoin;

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
import java.util.Arrays;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.junit.Ignore;

@Ignore
public class DummyXJoinResultsFactory implements XJoinResultsFactory<String> {
  
  private String string;
  
  private String[] values;
  
  // all join ids return a result *except* this one!
  private String missingId;
  
  @Override
  @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
    String valuesStr = (String)args.get("values");
    values = valuesStr != null ? valuesStr.split(",") : new String[0];
    string = (String)args.get("string");
    missingId = (String)args.get("missingId");
  }
  
  public String getMissingId() {
    return missingId;
  }
  
  public String getString() {
    return string;
  }

  @Override
  public XJoinResults<String> getResults(SolrParams params) throws IOException {
    String valuesStr = params != null ? (String)params.get("values") : null;
    return new Results(valuesStr != null ? valuesStr.split(",") : values);
  }
  
  public class Results implements XJoinResults<String> {

    private String[] values;
    
    private Results(String[] values) {
      this.values = values;
    }
    
    @Override
    public Object getResult(String joinId) {
      if (joinId.equals(missingId)) {
        return null;
      }
      return new Result(joinId);
    }

    @Override
    public Iterable<String> getJoinIds() {
      Arrays.sort(values);
      return Arrays.asList(values);
    }
    
    public String getString() {
      return string;
    }
    
  }
  
  public static class Result {
    
    private String value;
    
    private Result(String value) {
      this.value = value;
    }
    
    public String getValue() {
      return value;
    }
    
    public double getScore() {
      return TestXJoinValueSourceParser.value;
    }
    
  }

}
