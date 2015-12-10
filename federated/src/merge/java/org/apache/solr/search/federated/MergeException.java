package org.apache.solr.search.federated;

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

import org.apache.solr.schema.SchemaField;

@SuppressWarnings("serial")
public class MergeException extends RuntimeException {

  private SchemaField field;
  
  public MergeException(SchemaField field, String message) {
    super(message + ": " + field.getName());
    this.field = field;
  }
  
  public SchemaField getField() {
    return field;
  }
  
  public static class MissingRequiredField extends MergeException {
    
    public MissingRequiredField(SchemaField field) {
      super(field, "Required field has no value");
    }
    
  }
  
  public static class FieldNotMultiValued extends MergeException {
    
    public FieldNotMultiValued(SchemaField field) {
      super(field, "Field not multi-valued");
    }
    
  }
  
}
