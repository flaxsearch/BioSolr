package uk.co.flax.biosolr.pdbe.solr;

import java.io.IOException;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

public interface XJoinResultsFactory {
	
	@SuppressWarnings("rawtypes")
	public void init(NamedList args);

	public XJoinResults getResults(SolrParams params) throws IOException;

}
