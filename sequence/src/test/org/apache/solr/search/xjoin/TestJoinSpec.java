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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class TestJoinSpec {

  private JoinSpec.Iterable ranges = new JoinSpec.Iterable() {
    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Integer> iterator(String componentName) {
      String[] bits = componentName.split("-");
      final int m = new Integer(bits[0]);
      final int n = bits.length > 1 ? new Integer(bits[1]) : m;
      return new Iterator<Integer>() {
  
        int N = m;
        
        @Override
        public boolean hasNext() {
          return N <= n;
        }
  
        @Override
        public Integer next() {
          return N++;
        }
  
        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
        
      };
    }
  };

  private void assertParseResult(Integer[] values, String v) {
    JoinSpec<Integer> js = JoinSpec.parse(v);
    Iterator<Integer> it = js.iterator(ranges);
    List<Integer> list = new ArrayList<>();
    while (it.hasNext()) {
      list.add(it.next());
    }
    assertEquals(Arrays.asList(values), list);
  }
  
  @Test
  public void testUnary() {
    Integer[] values = { 1, 2, 3 };
    assertParseResult(values, "1-3");
  }
  
  @Test
  public void test() {
    Integer[] values = { 1, 5, 8 };
    assertParseResult(values, "(5-7 XOR 6-8) OR 1");
  }
  
}
