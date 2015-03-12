package org.apache.solr.xjoin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.xjoin.XJoinParameters;
import org.apache.solr.xjoin.XJoinResults;
import org.apache.solr.xjoin.XJoinSearchComponent;
import org.apache.solr.xjoin.XJoinValueSourceParser;
import org.junit.Test;

public class TestXJoinValueSourceParser extends LuceneTestCase {

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void test() throws SyntaxError, IOException {
		final double value = 1.5;
		final String joinField = "jj";
		final String joinValue = "jvalue";
				
		// test join results mapping our joinId to the joinValue (and only that)
		XJoinResults results = new XJoinResults() {
			@Override
			public Object getResult(String joinId) {
				if (joinValue.equals(joinId)) {
					return new Object() {
						@SuppressWarnings("unused")
						public double getFoo() {
							return value;
						}
					};
				}
				return null;
			}

			@Override
			public String[] getJoinIds() {
				// not used by XJoinValueSourceParser
				return null;
			}
		};

		// mock SolrQueryReqest with our join results in the context
		SolrQueryRequest sqr = mock(SolrQueryRequest.class);
		Map<Object, Object> context = new HashMap<>();
		context.put(XJoinSearchComponent.RESULTS_TAG, results);
		when(sqr.getContext()).thenReturn(context);
		
		// mock function qparser returning an argument of "foo"
		// (to match getFoo() in the xjoin results)
		FunctionQParser fqp = mock(FunctionQParser.class);
		when(fqp.parseArg()).thenReturn("foo");
		when(fqp.getReq()).thenReturn(sqr);

		// test index with a single document with joinField containing joinValue
	    Directory dir = newDirectory();
	    RandomIndexWriter iw = new RandomIndexWriter(random(), dir);
	    Document doc = new Document();
	    doc.add(newStringField(joinField, joinValue, Field.Store.NO));
	    iw.addDocument(doc);
	    DirectoryReader ir = iw.getReader();

	    // get AtomicReaderContent suitable for our test document
	    List<AtomicReaderContext> leaves = ir.leaves();
	    int idx = ReaderUtil.subIndex(0, leaves);
	    AtomicReaderContext leaf = leaves.get(idx);
	    
	    // initialise and test an XJoinValueSourceParser
		XJoinValueSourceParser vsp = new XJoinValueSourceParser();
		NamedList initArgs = new NamedList();
		initArgs.add(XJoinParameters.INIT_JOIN_FIELD, joinField);
		vsp.init(initArgs);
		ValueSource vs = vsp.parse(fqp);
		FunctionValues fv = vs.getValues(null, leaf);
		assertEquals(value, fv.doubleVal(0), 0);

		// clean up
	    ir.close();
	    iw.close();
	    dir.close();
	}
	
}
