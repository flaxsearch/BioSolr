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

import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;

/**
 * Test case for XJoin, setting up a core containing test documents.
 */
public abstract class AbstractXJoinTestCase extends SolrTestCaseJ4 {
  protected static int numberOfDocs = 0;

  @BeforeClass
  public static void beforeClass() throws Exception {
    initCore("solrconfig.xml", "schema.xml", "xjoin/solr");
    
    numberOfDocs = 0;
    for (String[] doc : DOCUMENTS) {
      assertNull(h.validateUpdate(adoc("id", Integer.toString(numberOfDocs), "colour", doc[0], "letter", doc[1], "letter", doc[2])));
      numberOfDocs++;
    }
    assertNull(h.validateUpdate(commit()));
  }

  final static String[][] DOCUMENTS = new String[][] { { "red", "alpha", "beta" }, { "green", "alpha", "gamma" }, { "blue", "delta", "gamma" }, { "pink", "delta", "gamma" }, { "blue", "epsilon", "zeta" } };

}
