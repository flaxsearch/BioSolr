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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.IndexableField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.schema.CopyField;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.ReturnFields;
import org.apache.solr.search.federated.DuplicateDocumentList;
import org.apache.solr.search.federated.FilterDJoinQParserSearchComponent;

/**
 * Inspect the result documents for merge parents, and merge the children.
 */
public class MergeSearchComponent extends FilterDJoinQParserSearchComponent {
  
  public static final String SHARD_INFO_PARAM = "shardInfo";
  
  // return whether to do a merge at all
  private boolean doMerge(ResponseBuilder rb) {
    SolrParams params = rb.req.getParams();
    if (! params.getBool(getName(), false)) {
      return false;
    }

    if (rb.stage != ResponseBuilder.STAGE_GET_FIELDS) {
      return false;
    }
    
    return true;
  }
  
  // whether to return shard info
  private boolean wantsShards(ResponseBuilder rb) {
    return rb.req.getParams().getBool(getName() + "." + SHARD_INFO_PARAM, false);
  }
  
  // need to ask distributed servers for source fields for all copy fields needed in the aggregator
  // also add in [shard] field if we want shard info
  @Override
  public void modifyRequest(ResponseBuilder rb, SearchComponent who, ShardRequest sreq) {
    // do the filterQParser stuff first
    super.modifyRequest(rb, who, sreq);
    
    if (! doMerge(rb)) {
      return;
    }
    
    if (wantsShards(rb)) {
      sreq.params.add(CommonParams.FL, "[shard]");
    }
    
    ReturnFields rf = rb.rsp.getReturnFields();
    if (rf.wantsAllFields()) {
      // we already have what we need since we ask for everything...
      return;
    }

    IndexSchema schema = rb.req.getCore().getLatestSchema();
    for (SchemaField field : schema.getFields().values()) {
      if (! rf.wantsField(field.getName())) {
        continue;
      }
      for (String source : schema.getCopySources(field.getName())) {
        if (rf.wantsField(source)) {
          continue;
        }
        sreq.params.add(CommonParams.FL, source);
      }
    }
  }
  
  @Override
  public void finishStage(ResponseBuilder rb) {
    if (! doMerge(rb)) {
      return;
    }

    try {
      mergeAndConvert(rb);
    } catch (RuntimeException e) {
      // remove response docs, leaving the error stack trace
      rb.rsp.getValues().remove("response");
      throw e;
    }
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void mergeAndConvert(ResponseBuilder rb) {
    IndexSchema schema = rb.req.getCore().getLatestSchema();
    ReturnFields rf = rb.rsp.getReturnFields();

    boolean wantsShards = rb.req.getParams().getBool(getName() + "." + SHARD_INFO_PARAM, false);

    SolrDocumentList docs = (SolrDocumentList)rb.rsp.getValues().get("response");
    for (SolrDocument parent : docs) {
      parent.remove(DuplicateDocumentList.MERGE_PARENT_FIELD);

      Set shardList = wantsShards ? new HashSet() : null;
      Float score = null;
      for (SolrDocument doc : parent.getChildDocuments()) {
        String shard = (String)doc.getFieldValue("[shard]");
        NamedList nl = null;
        if (shardList != null) {
          nl = new NamedList();
          nl.add("address", shard);
          shardList.add(nl);
        }
        
        for (String fieldName : doc.getFieldNames()) {
          Object value = doc.getFieldValue(fieldName);
          
          for (CopyField cf : schema.getCopyFieldsList(fieldName)) {
            SchemaField field = cf.getDestination();
            addConvertedFieldValue(shard, parent, value, field);
          }
          
          SchemaField field = schema.getFieldOrNull(fieldName);
          if (field == null) {
            // do nothing
          } else if (field.getName().equals("score")) {
            score = Math.max(score != null ? score : 0.0f, (Float)value);
            if (nl != null) {
              nl.add("score", score);
            }
          } else {
            addConvertedFieldValue(shard, parent, value, field);          
          }
        }
      }
      if (shardList != null) {
        parent.setField("[shard]", shardList);
      } else {
        parent.removeFields("[shard]");
      }
      if (score != null) {
        parent.setField("score", score);
      } else {
        parent.removeFields("score");
      }

      // check required fields are present, and then remove if non-stored
      for (SchemaField field : schema.getFields().values()) {
        Object value = parent.getFieldValue(field.getName());
        if (value == null) {
          value = field.getDefaultValue();
          if (value != null) {
            parent.setField(field.getName(), value);
          }
        }
        if (value == null && field.isRequired() && rf.wantsField(field.getName())) {
          throw new MergeException.MissingRequiredField(field);
        }
        if (! field.stored()) {
          parent.removeFields(field.getName());
        }
      }

      // remove child documents
      while (parent.getChildDocumentCount() > 0) {
        parent.getChildDocuments().remove(0);
      }
    }
  }

  private void convert(SchemaField field, Object value, Set<Object> valueSet) {
    IndexableField indexable = field.getType().createField(field, value, 1.0f);
    valueSet.add(field.getType().toObject(indexable));
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes", "serial" })
  private void addConvertedFieldValue(String shard, SolrDocument superDoc, Object shardValue, SchemaField field) {
    Object mergeValue = superDoc.getFieldValue(field.getName());

    if (field.getType() instanceof MergeAbstractFieldType) {
      Object newValue = ((MergeAbstractFieldType)field.getType()).merge(shard, mergeValue, shardValue);
      if (newValue != MergeAbstractFieldType.DEFAULT_MERGE_BEHAVIOUR) {
        superDoc.setField(field.getName(), mergeValue);
        return;
      }
    }
    
    // continue with the default merge behaviour...
    if (shardValue == null) {
      return;
    }
    
    Set newValues = new HashSet() {
      @Override
      public boolean add(Object value) {
        if (value == null) return false;
        return super.add(value);
      }
    };
    
    if (shardValue instanceof List) {
      for (Object value : (List)shardValue) {
        convert(field, value, newValues);
      }
    } else {
      convert(field, shardValue, newValues);
    }
    if (newValues.size() == 0) {
      return;
    }

    if (field.multiValued()) {
      Set set = (Set)mergeValue;
      if (set == null) {
        set = new HashSet();
        superDoc.setField(field.getName(), set);
      }
      set.addAll(newValues);
    } else {
      newValues.add(mergeValue);
      if (newValues.size() > 1) {
        throw new MergeException.FieldNotMultiValued(field);
      } else if (newValues.size() == 1) {
        superDoc.setField(field.getName(), newValues.iterator().next());
      }
    }
  }

}
