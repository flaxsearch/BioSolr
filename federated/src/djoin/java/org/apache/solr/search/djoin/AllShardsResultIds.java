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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.solr.handler.component.ShardDoc;

/**
 * When asked for values(), return a ShardDoc for every doc id for every shard.
 */
@SuppressWarnings("serial")
public class AllShardsResultIds extends HashMap<Object, ShardDoc> {

  private String[] shardAddresses;
  
  public AllShardsResultIds(String[] shardAddresses) {
    this.shardAddresses = shardAddresses;
  }
  
  @Override
  public Collection<ShardDoc> values() {
    List<ShardDoc> sdocs = new ArrayList<>();
    for (Object id : this.keySet()) {
      for (String shard : shardAddresses) {
        ShardDoc sdoc = new ShardDoc();
        sdoc.id = id;
        sdoc.shard = shard;
        sdocs.add(sdoc);
      }
    }
    return sdocs;
  }
  
}
