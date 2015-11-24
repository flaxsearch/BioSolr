package org.apache.solr.search.djoin.fieldtypes;

import org.apache.lucene.search.SortField;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.StrField;
import org.apache.solr.search.Sorting;

public class DJoinString extends StrField {

  @Override
  public SortField getSortField(SchemaField field, boolean reverse) {
    return Sorting.getStringSortField(field.getName(), reverse, field.sortMissingLast(), field.sortMissingFirst());
  }

}
