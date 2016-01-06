package uk.co.flax.examples.xjoin;

import java.io.IOException;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.xjoin.XJoinResults;
import org.apache.solr.search.xjoin.XJoinResultsFactory;

public class OfferXJoinResultsFactory implements XJoinResultsFactory<String> {

	@Override
	@SuppressWarnings("rawtypes")
	public void init(NamedList args) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public XJoinResults<String> getResults(SolrParams params) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
