package uk.co.flax.biosolr;

import org.apache.lucene.search.SortField;
import org.apache.solr.schema.SchemaField;

public class DJoinForgivingInteger extends DJoinAbstractFieldType {
	
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
