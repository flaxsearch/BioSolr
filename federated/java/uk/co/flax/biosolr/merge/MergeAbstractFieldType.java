package uk.co.flax.biosolr.merge;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.response.TextResponseWriter;
import org.apache.solr.schema.FieldType;

public abstract class MergeAbstractFieldType extends FieldType {

	@Override
	public void write(TextResponseWriter writer, String name, IndexableField f) throws IOException {
		// do nothing
	}
	
	/**
	 * Convert from a shard value.
	 */
	@Override
	protected IndexableField createField(String name, String val, org.apache.lucene.document.FieldType type, float boost) {
		return new ObjectField(name, convert(val), type, boost);
	}
	
	@Override
	public Object toObject(IndexableField f) {
		return ((ObjectField)f).object;
	}
	
	public abstract Object convert(String val);

	private class ObjectField implements IndexableField {

		private String name;
		
		private Object object;
		
		private IndexableFieldType type;
		
		private float boost;

		private ObjectField(String name, Object object, IndexableFieldType type, float boost) {
			this.name = name;
			this.object = object;
			this.type = type;
			this.boost = boost;
		}
		
		@Override
		public String name() {
			return name;
		}

		@Override
		public IndexableFieldType fieldType() {
			return type;
		}

		@Override
		public float boost() {
			return boost;
		}

		@Override
		public BytesRef binaryValue() {
			return null;
		}

		@Override
		public String stringValue() {
			return null;
		}

		@Override
		public Reader readerValue() {
			return null;
		}

		@Override
		public Number numericValue() {
			return null;
		}

		@Override
		public TokenStream tokenStream(Analyzer analyzer, TokenStream reuse) throws IOException {
			return null;
		}
		
	}
	
}
