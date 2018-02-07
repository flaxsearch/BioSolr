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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.solr.search.xjoin.NameConverter;
import org.junit.Test;

public class TestNameConverter {

  @Test
  public void fieldToMethod() {
    assertEquals("getFooBar", NameConverter.getMethodName("foo_bar"));
    assertEquals("getAProperty", NameConverter.getMethodName("a_property"));
  }
  
  @Test
  public void methodToField() {
    assertEquals("foo_bar", NameConverter.getFieldName("isFooBar"));
    assertEquals("a_property", NameConverter.getFieldName("getAProperty"));
  }
  
  @Test
  public void nulls() {
    assertNull(NameConverter.getFieldName("doSomething"));
  }
  
}
