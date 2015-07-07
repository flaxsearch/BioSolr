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

import static org.apache.commons.collections.IteratorUtils.toList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class TestCombinations {

  Iterator<Integer> e, a, aa, b, bb;
  Integer[] es = { };
  Integer[] as = { 3, 4, 6, 9 };
  Integer[] aas = { 3, 4, 5, 6, 9 };
  Integer[] bs = { 1, 2, 5, 8 };
  Integer[] bbs = { 1, 2, 3, 5, 8 };
  Integer[] cs = { 1, 2, 3, 4, 5, 6, 8, 9 };
  Integer[] ds = { 3, 5 };
  Integer[] xs = { 1, 2, 4, 6, 8, 9 };
  Integer[] ns = { 4, 6, 9 };
  
  @Before
  public void setUp() {
    e = Arrays.asList(es).iterator();
    a = Arrays.asList(as).iterator();
    aa = Arrays.asList(aas).iterator();
    b = Arrays.asList(bs).iterator();
    bb = Arrays.asList(bbs).iterator();
  }
  
  @Test
  public void testOr() {
    List<Integer> cl = toList(Combinations.or(a, b));
    assertEquals(Arrays.asList(cs), cl);
  }
  
  @Test
  public void testOrRepeat() {
    List<Integer> cl = toList(Combinations.or(a, bb));
    assertEquals(Arrays.asList(cs), cl);
  }
  
  @Test
  public void testOrRepeat2() {
    List<Integer> cl = toList(Combinations.or(aa, b));
    assertEquals(Arrays.asList(cs), cl);
  }
  
  @Test
  public void testOrEmptyA() {
    List<Integer> cl = toList(Combinations.or(e, b));
    assertEquals(Arrays.asList(bs), cl);
  }
  
  @Test
  public void testOrEmptyB() {
    List<Integer> cl = toList(Combinations.or(a, e));
    assertEquals(Arrays.asList(as), cl);
  }
  
  @Test
  public void testOrEmptyAB() {
    List<Integer> cl = toList(Combinations.or(e, e));
    assertEquals(Arrays.asList(es), cl);
  }
  
  @Test
  public void testAnd() {
    List<Integer> cl = toList(Combinations.and(aa, bb));
    assertEquals(Arrays.asList(ds), cl);    
  }
  
  @Test
  public void testAndEmptyA() {
    List<Integer> cl = toList(Combinations.and(e, bb));
    assertEquals(Arrays.asList(es), cl);    
  }
  
  @Test
  public void testAndEmptyB() {
    List<Integer> cl = toList(Combinations.and(aa, e));
    assertEquals(Arrays.asList(es), cl);    
  }
  
  @Test
  public void testAndEmptyAB() {
    List<Integer> cl = toList(Combinations.and(e, e));
    assertEquals(Arrays.asList(es), cl);    
  }
  
  @Test
  public void testXOr() {
    List<Integer> cl = toList(Combinations.xor(a, b));
    assertEquals(Arrays.asList(cs), cl);
  }
  
  @Test
  public void testXOrRepeat() {
    List<Integer> cl = toList(Combinations.xor(aa, bb));
    assertEquals(Arrays.asList(xs), cl);
  }
  
  @Test
  public void testAndNot() {
    List<Integer> cl = toList(Combinations.andNot(a, b));
    assertEquals(Arrays.asList(as), cl);
  }
  
  @Test
  public void testAndNotRepeat() {
    List<Integer> cl = toList(Combinations.andNot(aa, bb));
    assertEquals(Arrays.asList(ns), cl);
  }

  // more of an instructive example of how this will be used in XJoinQParserPlugin
  @Test
  public void testHetero() {
    List<Foo> A_36 = Foo.range("A", 3, 6);
    List<Foo> A_48 = Foo.range("A", 4, 8);
    List<Foo> B_17 = Foo.range("B", 1, 7);
    
    // so, different types shouldn't get mingled
    List<Foo> A_36_B_17 = Foo.range("A", 3, 6);
    A_36_B_17.addAll(Foo.range("B", 1, 7));
    assertEquals(A_36_B_17, toList(Combinations.or(A_36.iterator(), B_17.iterator())));
    assertEquals(0, toList(Combinations.and(A_36.iterator(), B_17.iterator())).size());
    
    // but check that the same type does
    List<Foo> A_38 = Foo.range("A", 3, 8);
    assertEquals(A_38, toList(Combinations.or(A_36.iterator(), A_48.iterator())));
    
    Iterator<Foo> A_or_B = Combinations.or(A_48.iterator(), B_17.iterator());
    assertEquals(3, toList(Combinations.and(A_36.iterator(), A_or_B)).size());
  }

  static class Foo implements Comparable<Foo> {
    String type;
    int value;
    
    static List<Foo> range(String type, int m, int n) {
      List<Foo> f = new ArrayList<Foo>(n - m + 1);
      for (int i = 0; i <= n - m; ++i) {
        Foo foo = new Foo();
        foo.type = type;
        foo.value = m + i;
        f.add(foo);
      }
      return f;
    }

    @Override
    public int compareTo(Foo that) {
      int v = this.type.compareTo(that.type);
      if (v != 0) {
        return v;
      }
      return new Integer(this.value).compareTo(that.value);
    }
    
    @Override
    public boolean equals(Object that) {
      return that instanceof Foo && compareTo((Foo)that) == 0;
    }
  }

}
