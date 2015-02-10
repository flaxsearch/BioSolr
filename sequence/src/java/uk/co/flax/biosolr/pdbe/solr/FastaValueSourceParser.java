package uk.co.flax.biosolr.pdbe.solr;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.DoubleDocValues;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.ValueSourceParser;

import uk.co.flax.biosolr.pdbe.Alignment;
import uk.co.flax.biosolr.pdbe.FastaJobResults;

public class FastaValueSourceParser extends ValueSourceParser {
	
	public static final String INIT_JOIN_FIELD = "joinField";
	
	private String joinField;
	
	@Override
	@SuppressWarnings("rawtypes")
	public void init(NamedList args) {
		super.init(args);
		
		joinField = (String)args.get(INIT_JOIN_FIELD);
	}
	
	@Override
	public ValueSource parse(FunctionQParser fqp) throws SyntaxError {
		FastaJobResults results = (FastaJobResults)fqp.getReq().getContext().get(ExternalResultsSearchComponent.RESULTS_TAG);
		if (results == null) {
			throw new RuntimeException("No FASTA job results in request context");
		}
		return new FastaValueSource(results, fqp.parseArg());
	}
	
	public class FastaValueSource extends ValueSource {

		private FastaJobResults results;
		private String field;

		public FastaValueSource(FastaJobResults results, String field) {
			this.results = results;
			this.field = field;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public FunctionValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
			final BinaryDocValues pdbIdChains = FieldCache.DEFAULT.getTerms(readerContext.reader(), joinField, true);

			return new DoubleDocValues(this) {

				@Override
				public double doubleVal(int docId) {
					BytesRef pdbIdChain = pdbIdChains.get(docId);
					if (pdbIdChain == null) {
						throw new RuntimeException("No pdbIdChain for docId: " + docId);
					}
					Alignment a = results.getResult(pdbIdChain.utf8ToString());
					if (a == null) {
						throw new RuntimeException("Unknown alignment: " + pdbIdChain.utf8ToString());
					}
					if ("evalue".equals(field)) {
						return a.getEValue();
					} else if ("length".equals(field)) {
						return a.getChain().length();
					} else {
						throw new RuntimeException("No such field: " + field);
					}
				}
				
			};
		}
		
		@Override
		public String description() {
			return "$description$";
		}

		@Override
		public boolean equals(Object object) {
			if (! (object instanceof FastaValueSource)) {
				return false;
			}
			return results.equals(((FastaValueSource)object).results);
		}

		@Override
		public int hashCode() {
			return results.hashCode();
		}
		
	}

}
