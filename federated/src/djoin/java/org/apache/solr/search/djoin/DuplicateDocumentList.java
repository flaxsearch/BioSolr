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
  public static String SORT_VALUE_FIELD = "sortValue";

  public DuplicateDocumentList(int initialSize, Float maxScore, long numFound, int offset) {
    for (int i = 0; i < initialSize; ++i) {
      add(null);
    }
    if (maxScore != null) {
      setMaxScore(maxScore);
    }
    setNumFound(numFound);
    setStart(offset);
  }
  
  @Override
  public SolrDocument set(int index, SolrDocument doc) {
    get(index).addChildDocument(doc);
    return null;
  }
  
  public void setParentDoc(int index, Object sortValue, float score) {
    SolrDocument parent = new SolrDocument();
    parent.setField(MERGE_PARENT_FIELD, true);
    parent.setField("score", score);
    if (sortValue != null) {
      parent.setField(SORT_VALUE_FIELD, sortValue);
    }
    super.set(index, parent);
  }

}
