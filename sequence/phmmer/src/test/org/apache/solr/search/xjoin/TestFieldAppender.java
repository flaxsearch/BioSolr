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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.xjoin.FieldAppender;
import org.junit.Test;

public class TestFieldAppender {

  @Test
  public void fieldList() {
    FieldAppender fa = new FieldAppender(" foo  bar,  ,  baz, foo ");
    String[] expected = new String[] { "foo", "bar", "baz" };
    assertEquals(new HashSet<>(Arrays.asList(expected)), fa.getFieldNames());
  }

  @Test
  public void fieldListAll() {
    FieldAppender fa = new FieldAppender("foo, * bar");
    assertNull(fa.getFieldNames());
  }
  
  @Test
  public void addFieldNames() {
    FieldAppender fa = new FieldAppender();
    fa.getFieldNames().add("foo");
    fa.getFieldNames().add("bar");
    String[] expected = new String[] { "foo", "bar" };
    assertEquals(new HashSet<>(Arrays.asList(expected)), fa.getFieldNames());
    fa.appendAllFields();
    assertNull(fa.getFieldNames());
  }
  
  @Test
  @SuppressWarnings("rawtypes")
  public void addNamedList() {
    FieldAppender fa = new FieldAppender(true);
    NamedList root = new NamedList();
    NamedList added = fa.addNamedList(root, "list", new Object() {
      @SuppressWarnings("unused")
      public String getFoo() {
        return "foo";
      }
      
      @SuppressWarnings("unused")
      public int getBar() {
        return 123;
      }
      
      @SuppressWarnings("unused")
      public boolean isBaz() {
        return true;
      }
    });
    assertEquals(added, root.get("list"));
    assertEquals("foo", added.get("foo"));
    assertEquals(123, added.get("bar"));
    assertTrue((boolean)added.get("baz"));
  }
  
}
