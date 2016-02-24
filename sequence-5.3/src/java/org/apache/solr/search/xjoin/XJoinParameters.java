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

import org.apache.solr.common.params.CommonParams;

/**
 * Parameters for XJoin functionality.
 */
public class XJoinParameters {

  // XJoinSearchComponent parameters
  public static final String INIT_RESULTS_FACTORY = "factoryClass";
  public static final String INIT_JOIN_FIELD = "joinField";
  public static final String EXTERNAL_PREFIX = "external";
  public static final String RESULTS_FIELD_LIST = "results";
  public static final String DOC_FIELD_LIST = CommonParams.FL;

  // XJoinValueSourceParser parameters
  public static final String INIT_XJOIN_COMPONENT_NAME = "xJoinSearchComponent";
  public static final String INIT_ATTRIBUTE = "attribute";
  public static final String INIT_DEFAULT_VALUE = "defaultValue";
  public static final String INIT_FIELD_DEFAULT_VALUE = "fieldDefaultValue";
  
  // XJoinQParserPlugin parameters
  public static final String INIT_FIELD = "f";
  
}
