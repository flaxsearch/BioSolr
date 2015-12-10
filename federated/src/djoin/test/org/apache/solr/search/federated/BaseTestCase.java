package org.apache.solr.search.federated;

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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.TestHarnessWrapper;
import org.xml.sax.SAXException;

import sun.misc.IOUtils;

public abstract class BaseTestCase extends SolrTestCaseJ4 {

  public static void loadShardCore(String coreName, String[][] documents, String... fields) throws SAXException {
    TestHarnessWrapper w = new TestHarnessWrapper(h, coreName);
    for (String[] doc : documents) {
      List<String> fieldsAndValues = new ArrayList<>(2 * doc.length);
      for (int i = 0; i < doc.length; ++i) {
        if (doc[i] == null) continue;
        for (String value : doc[i].split(",")) {
          fieldsAndValues.add(fields[i]);
          fieldsAndValues.add(value);
        }
      }
      assertNull(w.validateUpdate(adoc(fieldsAndValues.toArray(new String[fieldsAndValues.size()]))));
    }
    assertNull(w.validateUpdate(commit()));
  }
  
  public static void initCores(String solrXml, String solrHome) throws Exception {
    try (InputStream in = ClassLoader.getSystemResourceAsStream(solrXml)) {
      byte[] encoded = IOUtils.readFully(in, -1, true);
      createCoreContainer(solrHome, new String(encoded, "UTF-8"));
    }
  }
  
  public static SolrQueryResponse query(SolrCore core, String handlerName, SolrParams params) {
    SolrQueryResponse rsp = new SolrQueryResponse();
    SolrQueryRequest req = new SolrQueryRequestBase(core, params) { };
    try {
      SolrRequestHandler handler = core.getRequestHandler(handlerName);
      core.execute(handler, req, rsp);
      return rsp;
    } finally {
      req.close();
    }
  }
  
  public static SolrDocumentList queryDocs(SolrCore core, String handlerName, SolrParams params) {
    SolrQueryResponse rsp = query(core, handlerName, params);
    assertNull(rsp.getException());
    return (SolrDocumentList)rsp.getValues().get("response");
  }
  
  public static void queryThrow(SolrCore core, String handlerName, SolrParams params) throws Exception {
    SolrQueryResponse rsp = query(core, handlerName, params);
    if (rsp.getException() != null) {
      throw rsp.getException();
    }
  }

}
