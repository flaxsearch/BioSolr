package uk.co.flax.biosolr.pdbe.solr;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

public class XJoinValueSourceParser extends ValueSourceParser {
	
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
		XJoinResults results = (XJoinResults)fqp.getReq().getContext().get(XJoinResultsSearchComponent.RESULTS_TAG);
		if (results == null) {
			throw new RuntimeException("No external results in request context");
		}
		return new ExternalValueSource(results, fqp.parseArg());
	}
	
	public class ExternalValueSource extends ValueSource {

		private XJoinResults results;
		private String methodName;

		public ExternalValueSource(XJoinResults results, String arg) {
			this.results = results;
			this.methodName = "get" + arg.substring(0, 1).toUpperCase() + arg.substring(1);
		}

		@Override
		@SuppressWarnings("rawtypes")
		public FunctionValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
			final BinaryDocValues joinValues = FieldCache.DEFAULT.getTerms(readerContext.reader(), joinField, true);

			return new DoubleDocValues(this) {

				@Override
				public double doubleVal(int doc) {
					BytesRef joinValue = joinValues.get(doc);
					if (joinValue == null) {
						throw new RuntimeException("No joinValue for doc: " + doc);
					}
					Object result = results.getResult(joinValue.utf8ToString());
					if (result == null) {
						throw new RuntimeException("Unknown result: " + joinValue.utf8ToString());
					}
					try {
						Method method = result.getClass().getMethod(methodName);
						return (Double)method.invoke(result);
					} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						throw new RuntimeException(e);
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
			if (! (object instanceof ExternalValueSource)) {
				return false;
			}
			return results.equals(((ExternalValueSource)object).results);
		}

		@Override
		public int hashCode() {
			return results.hashCode();
		}
		
	}

}
