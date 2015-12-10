package org.apache.solr.search.federated;

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
