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

/**
 * Interface for external process results.
 */
public interface XJoinResults<IdType> {
  
  /**
   * Get the external process result with given join id string.
   * 
   * Note: you might need to convert the argument to the correct type.
   */
  Object getResult(String joinIdStr);
  
  /**
   * Get an ordered (ascending) iterable of external process join ids (null is
   * not a valid id).
   */
  Iterable<IdType> getJoinIds();

}
