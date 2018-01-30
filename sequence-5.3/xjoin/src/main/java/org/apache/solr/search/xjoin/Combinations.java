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
import java.util.NoSuchElementException;

public abstract class Combinations<T> implements Iterator<T> {

  Iterator<T> a, b;

  boolean useA = false;
  boolean fromA = true;
  int repeat = 0;
  T target, value;

  abstract T advance();

  Combinations(Iterator<T> a, Iterator<T> b) {
    this.a = a;
    this.b = b;
    target = a.hasNext() ? a.next() : null;
    value = advance();
  }

  @Override
  public boolean hasNext() {
    return value != null;
  }

  @Override
  public T next() {
    if (value == null) {
      throw new NoSuchElementException();
    }
    try {
      return value;
    } finally {
      value = advance();
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  public static <T extends Comparable<T>> Iterator<T> or(Iterator<T> first, Iterator<T> second) {
    return new Combinations<T>(first, second) {

      @Override
      protected T advance() {
        Iterator<T> it = useA ? a : b;
        while (true) {
          T value = it.hasNext() ? it.next() : null;
          int cmp = 1;
          if (value == null || (target != null && (cmp = value.compareTo(target)) >= 0)) {
            if (cmp == 0) {
              continue;
            }
            try {
              return target;
            } finally {
              target = value;
              useA = !useA;
            }
          }
          return value;
        }
      }

    };
  }

  public static <T extends Comparable<T>> Iterator<T> and(Iterator<T> first, Iterator<T> second) {
    return new Combinations<T>(first, second) {

      @Override
      protected T advance() {
        while (true) {
          Iterator<T> it = useA ? a : b;
          if (!it.hasNext()) {
            return null;
          }
          T value = it.next();
          if (target == null) {
            return null;
          }
          int cmp = value.compareTo(target);
          if (cmp < 0) {
            continue;
          }
          try {
            if (cmp == 0) {
              return target;
            }
          } finally {
            target = value;
            useA = !useA;
          }
        }
      }

    };
  }

  public static <T extends Comparable<T>> Iterator<T> andNot(Iterator<T> first, Iterator<T> second) {
    return new Combinations<T>(first, second) {

      @Override
      protected T advance() {
        while (true) {
          Iterator<T> it = useA ? a : b;
          T value = it.hasNext() ? it.next() : null;
          int cmp = 1;
          if (value == null || (target != null && (cmp = value.compareTo(target)) >= 0)) {
            if (cmp == 0) {
              repeat = 1;
            }
            try {
              if (repeat == 0) {
                if (fromA) {
                  return target;
                } else {
                  continue;
                }
              }
            } finally {
              target = value;
              fromA = useA;
              useA = !useA;
            }
            if (repeat++ == 2) {
              repeat = 0;
            }
          } else {
            if (useA)
              return value;
          }
        }
      }

    };
  }

  public static <T extends Comparable<T>> Iterator<T> xor(Iterator<T> first, Iterator<T> second) {
    return new Combinations<T>(first, second) {

      @Override
      protected T advance() {
        while (true) {
          Iterator<T> it = useA ? a : b;
          T value = it.hasNext() ? it.next() : null;
          int cmp = 1;
          if (value == null || (target != null && (cmp = value.compareTo(target)) >= 0)) {
            if (cmp == 0) {
              repeat = 1;
            }
            try {
              if (repeat == 0) {
                return target;
              }
            } finally {
              target = value;
              useA = !useA;
            }
            if (repeat++ == 2) {
              repeat = 0;
            }
          } else {
            return value;
          }
        }
      }

    };
  }

}
