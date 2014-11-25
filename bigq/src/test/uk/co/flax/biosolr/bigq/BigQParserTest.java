package uk.co.flax.biosolr.bigq;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BigQParserTest {

	private String qstr;
	private ModifiableSolrParams localParams;
	private SolrParams params;
	private SolrQueryRequest req;
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setup() throws Exception {
		qstr = "134^01.7,612^23.1,68^12.3,12^12.2,635^0.26";
		localParams = new ModifiableSolrParams();
		params = new ModifiableSolrParams();
		
		req = mock(SolrQueryRequest.class);
		IndexSchema schema = mock(IndexSchema.class);
		SchemaField field = new SchemaField("id", mock(FieldType.class));
		when(req.getSchema()).thenReturn(schema);
		when(schema.getUniqueKeyField()).thenReturn(field);
		when(schema.getQueryParserDefaultOperator()).thenReturn("AND");
		
		ContentStream content = mock(ContentStream.class);
		List<ContentStream> list = new ArrayList<ContentStream>();
		list.add(content);
		when(content.getStream()).thenReturn(new ByteArrayInputStream(qstr.getBytes("UTF-8")));
		when(req.getContentStreams()).thenReturn(list);
	}
	
	@Test
	public void doTest() throws Exception {
		localParams.add("operator", "OR");
		localParams.add("field", "id");
		
		QParser parser = new BigQParser(qstr, localParams, params, req);
		String query = parser.parse().toString();
		assertEquals("id:134^1.7 id:612^23.1 id:68^12.3 id:12^12.2 id:635^0.26", query);
	}

	@Test
	public void doTest_requestBody() throws Exception {
		localParams.add("operator", "OR");
		localParams.add("field", "id");

		QParser parser = new BigQParser("", localParams, params, req);
		String query = parser.parse().toString();
		assertEquals("id:134^1.7 id:612^23.1 id:68^12.3 id:12^12.2 id:635^0.26", query);
	}

	@Test
	public void doTest_defaults() throws Exception {
		QParser parser = new BigQParser(qstr, localParams, params, req);
		String query = parser.parse().toString();
		assertEquals("+id:134^1.7 +id:612^23.1 +id:68^12.3 +id:12^12.2 +id:635^0.26", query);
	}

	@Test
	public void doTest_syntaxError() throws Exception {
		exception.expect(SyntaxError.class);
		QParser parser = new BigQParser("1^2^3", localParams, params, req);
		parser.parse();
	}
	
}
