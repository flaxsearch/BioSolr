package uk.co.flax.biosolr.bigq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.parser.QueryParser;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

/**
 * So the idea is you can do:
 * 
 * fq={!bigq operator=OR field=id}134^1.7,612^23.1,68^12.3,12^12.2,635^0.26
 * 
 * (here we are applying boosts to each value)
 * 
 * or
 * 
 * q={!bigq operator=AND field=uid}
 * 
 * (here since there is no value, use the request body as input)
 * 
 * and a query or filter query is generated from the input.
 * 
 * Consider allowing a binary format in the request body.
 * 
 * If operator isn't specified, use the default operator. If field isn't specified, use unique id field.
 */
public class BigQParser extends QParser {

	public static final String FIELD_PARAM = "field";
	public static final String OPERATOR_PARAM = "operator";
	
	public BigQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
		super(qstr, localParams, params, req);
	}
	
	@Override
	public Query parse() throws SyntaxError {
		String defaultField = req.getSchema().getUniqueKeyField().getName();
		String field = localParams.get(FIELD_PARAM, defaultField);
		
		String defaultOperator = req.getSchema().getQueryParserDefaultOperator();
		String operatorStr = localParams.get(OPERATOR_PARAM, defaultOperator);
		QueryParser.Operator qpOperator = QueryParser.Operator.valueOf(operatorStr);
		BooleanClause.Occur operator = qpOperator == QueryParser.Operator.AND ? BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD;
		
		if (qstr.length() == 0) {
			//FIXME: stream request body
			//TODO: use content types to determine how to parse content
			if (req.getContentStreams() != null) {
				StringBuffer s = new StringBuffer();
				for (ContentStream content : req.getContentStreams()) {
					try {
				        BufferedReader reader = new BufferedReader(new InputStreamReader(content.getStream()));
				        String line;
				        while ((line = reader.readLine()) != null) {
				            s.append(line);
				        }
	 				} catch (IOException e) {
						//FIXME: how does SOLR prefer this to be handled?
						throw new RuntimeException(e);
					}
				}
				qstr = s.toString();
			}
		}
		
		BooleanQuery query = new BooleanQuery(true);
		String[] keys = qstr.split(",");
		for (String key: keys) {
			key = key.trim();
			int i = key.indexOf('^');
			String value = i != -1 ? key.substring(0, i) : key;
			String boost = i != -1 ? key.substring(i + 1) : null;
			Query q = new TermQuery(new Term(field, value));
			if (boost != null) {
				try {
					q.setBoost(Float.valueOf(boost));
				} catch (NumberFormatException e) {
					throw new SyntaxError("Could not parse key '" + key + "': " + e.getMessage());
				}
			}
			query.add(q, operator);
		}
		return query;
	}

}
