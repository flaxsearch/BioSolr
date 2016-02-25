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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

/**
 * Class for adding properties from an object to a NamedList.
 */
public class FieldAppender {

  // selected properties (or null for all)
  private Set<String> fieldNames;
  
  /**
   * Create a FieldAppender for adding the fields specified by the given
   * SOLR formatted field list (i.e. command or space delimited).
   */
  public FieldAppender(String fl) {
    this();
    for (String field : fl.split("[, ]")) {
      field = field.trim();
      if (field.length() == 0) {
        continue;
      }
      if ("*".equals(field)) {
        fieldNames = null;
        return;
      }
      fieldNames.add(field);
    }
  }

  /**
   * Create a FieldAppender for adding all fields (if the parameter is true)
   * or for no fields (but fields may be added via getFieldNames()).
   */
  public FieldAppender(boolean all) {
    fieldNames = all ? null : new HashSet<String>();
  }
  
  /**
   * Create a FieldAppeneder for adding fields, which may be added via
   * getFieldNames().
   */
  public FieldAppender() {
    this(false);
  }
  
  /**
   * Returns the (modifiable) set of field names to be added. A value of null
   * indicates all fields are to be added.
   */
  public Set<String> getFieldNames() {
    return fieldNames;
  }
  
  /**
   * Indicate that all fields should be added. Once this method is called, fields
   * can not be added using the set returned by getFieldNames() (since it is null).
   */
  public void appendAllFields() {
    fieldNames = null;
  }
  
  /**
   * Add a NamedList (with given name) with properties from the given object.
   * Returns the new NamedList.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public NamedList addNamedList(NamedList target, String name, Object object) {
    NamedList<Object> list = new SimpleOrderedMap<>();
    target.add(name, list);

    if (object instanceof Map) {
      Map map = (Map)object;
      for (Object field : map.keySet()) {
        String fieldName = field.toString();
        if (! includeField(fieldName)) continue;
        list.add(fieldName, map.get(field));
      }
    } else {
      for (Method method : object.getClass().getMethods()) {
        if (method.getParameterTypes().length > 0) continue;
        String fieldName = NameConverter.getFieldName(method.getName());
        if (! includeField(fieldName)) continue;
        try {
          list.add(fieldName, method.invoke(object));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
          throw new RuntimeException(e.getClass().getName() + " (" + fieldName + ": " + e.getMessage() + ")", e.getCause());
        }
      }
    }
    
    return list;
  }
  
  // whether to include a particular object field based on fieldNames
  private boolean includeField(String fieldName) {
    if (fieldName == null) return false;
    if (fieldNames == null) {
      // return all get methods except getClass()
      if (fieldName.equals("class")) return false;
    } else {
      // return named methods only
      if (! fieldNames.contains(fieldName)) return false;
    }
    return true; 
  }

}
