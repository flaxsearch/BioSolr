package uk.co.flax.biosolr.bigq;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

public class BigQParserPlugin extends QParserPlugin {

	@Override
	public void init(@SuppressWarnings("rawtypes") NamedList args) {
		// no parameters to parse
	}

	@Override
	public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
		return new BigQParser(qstr, localParams, params, req);
	}

}
