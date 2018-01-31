package org.apache.solr.search.xjoin;

import java.util.Locale;

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
 * Utility methods for converting between method names and field names.
 */
public class NameConverter {
  
  /**
   * Given a method name, generate a field name if it's a getXxx or isXxx method.
   * (Otherwise, return null.) The name is lower cased and _ seperated, so e.g.
   * getFooBar becomes foo_bar.
   */
  public static String getFieldName(String methodName) {
    int i;
        if (methodName.startsWith("get")) {
          i = 3;
        } else if (methodName.startsWith("is")) {
          i = 2;
        } else {
          return null;
        }
        StringBuilder fieldName = new StringBuilder();
        for (; i < methodName.length(); ++i) {
          char c = methodName.charAt(i);
          if (Character.isUpperCase(c)) {
            if (fieldName.length() > 0) {
              fieldName.append("_");
            }
            fieldName.append(Character.toLowerCase(c));
          } else {
            fieldName.append(c);
          }
        }
        return fieldName.toString();    
  }
  
  /**
   * Given a field name, generate a method name. The name is CamelCased, so
   * e.g. foo_bar becomes getFooBar.
   */
  public static String getMethodName(String fieldName) {
    StringBuilder methodName = new StringBuilder("get");
    for (String bit : fieldName.split("_")) {
      // English locale since code is written in English
      methodName.append(bit.substring(0, 1).toUpperCase(Locale.ENGLISH));
      methodName.append(bit.substring(1));
    }
    return methodName.toString();
  }

}
