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

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 * Return docs with the same index by storing as child documents
 * of a parent, which is marked up with a field indicating that
 * it is a merge parent.
 */
@SuppressWarnings("serial")
public class DuplicateDocumentList extends SolrDocumentList {
  
  public static String MERGE_PARENT_FIELD = "__merge_parent__";
  
  @Override
  public SolrDocument set(int index, SolrDocument doc) {
    SolrDocument old = get(index);
    if (old == null) {
      SolrDocument parent = new SolrDocument();
      parent.setField(MERGE_PARENT_FIELD, true);
      parent.addChildDocument(doc);
      super.set(index, parent);
    } else {
      old.addChildDocument(doc);
    }
    return null;
  }

}
