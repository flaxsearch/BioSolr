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

import java.util.Iterator;

public class JoinSpec<T extends Comparable<T>> {
  
  private enum Op {
    xjoin, unary, or, and, xor, and_not
  }

  Op operator = Op.unary;
  
  JoinSpec<T> first, second; // both are be null when operator == xjoin

  String functionName; // for e.g. fn(component_name)
  
  String componentName; // only non-null when operator == xjoin
  
  JoinSpec<T> parent;
  
  private JoinSpec(JoinSpec<T> parent) {
    this.parent = parent;
    this.functionName = null;
  }
  
  private void add(JoinSpec<T> child) {
    if (first == null) {
      first = child;
    } else if (second == null) {
      second = child;
    } else {
      throw new RuntimeException("Bad operator (programming error)");
    }
  }
  
  public Iterator<T> iterator(Iterable its) {
    if (operator == Op.xjoin) {
      return its.iterator(componentName);
    }
    Iterator<T> first = this.first.iterator(its);
    if (operator == Op.unary) {
      return first;
    }
    Iterator<T> second = this.second.iterator(its);
    switch (operator) {
    case or:
      return Combinations.or(first, second);
    case and:
      return Combinations.and(first, second);
    case xor:
      return Combinations.xor(first, second);
    case and_not:
      return Combinations.andNot(first, second);
    default:
      throw new RuntimeException("Bad operator: " + operator);
    }
  }
  
  public static <T extends Comparable<T>> JoinSpec<T> parse(String v) {
    // ((a OR b) AND fn(c)) XOR (d AND NOT e)
    JoinSpec<T> spec = new JoinSpec<>(null);
    for (int i = 0; i < v.length(); ++i) {
      char c = v.charAt(i);
      if (Character.isWhitespace(c)) {
        continue;
      }
      int z = 0;
      if (c == '(') {
        JoinSpec<T> js = new JoinSpec<>(spec);
        spec.add(js);
        spec = js;
      } else if (c == ')') {
        spec = spec.parent;
      } else if ((z = safeCmp(v, i, "OR")) > 0) {
          spec.operator = Op.or;
      } else if ((z = safeCmp(v, i, "AND NOT")) > 0) {
          spec.operator = Op.and_not;
      } else if ((z = safeCmp(v, i, "AND")) > 0) {
          spec.operator = Op.and;
      } else if ((z = safeCmp(v, i, "XOR")) > 0) {
          spec.operator = Op.xor;
      } else {
        // it must be a component name (or function of) until the next whitespace or unmatched )
        int j, k = -1, l = -1;
        for (j = i; j < v.length(); ++j) {
          char cc = v.charAt(j);
          if (cc == '(') {
            if (k == -1) {
              k = j;
            } else {
              throw new RuntimeException("Multiple open brackets in fn(name) whilst parsing JoinSpec");
            }
          } else if (cc == ')') {
            if (k != -1) {
              l = j;
            } else {
              break;
            }
          }
          else if (Character.isWhitespace(cc)) {
            break;
          }
        }
        JoinSpec<T> js = new JoinSpec<>(spec);
        js.operator = Op.xjoin;
        if (k != -1) {
          if (l == -1) {
            throw new RuntimeException("Unmatched brackets in fn(name) whilst parsing JoinSpec");
          }
          js.functionName = v.substring(i, k);
          js.componentName = v.substring(k + 1, l);
        } else {
          js.componentName = v.substring(i, j);
        }
        spec.add(js);
        i = j - 1;
      }
      i += z;
    }
    return spec;
  }
  
  private static int safeCmp(String v, int i, String t) {
    int j = i + t.length();
    boolean match = j < v.length() && v.substring(i, j).equals(t);
    return match ? t.length() : 0;
  }
  
  public interface Iterable {
    <T extends Comparable<T>> Iterator<T> iterator(String componentName);
  }
  
  public interface Function {
    Object fn(Object x);
  }
  
}
