package org.apache.solr.search.djoin.fieldtypes;

import org.apache.lucene.search.SortField;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.djoin.MergeAbstractFieldType;

public class ForgivingInteger extends MergeAbstractFieldType {
	
	@Override
	public Object convert(String val) {
		try {
			return new Integer(val);
		} catch (NumberFormatException e) {
			return Float.NaN;
		}
	}

	@Override
	public SortField getSortField(SchemaField field, boolean reverse) {
	    field.checkSortability();
	    return new SortField(field.getName(), SortField.Type.INT, reverse);
	}
	
}
