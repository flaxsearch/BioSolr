package uk.co.flax.biosolr.djoin;

import java.io.IOException;
import java.util.Set;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SyntaxError;

public class FilterQParserSearchComponent extends SearchComponent {

	public static final String COMPONENT_NAME = "djoin";

	// initialisation parameters
	public static final String INIT_QPARSER = "qParser";
	
	private String qParser;

  @Override
  @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
  	super.init(args);
  
  	qParser = (String)args.get(INIT_QPARSER);
  }

  @Override
  public void prepare(ResponseBuilder rb) throws IOException {
    // do nothing
  }

  @Override
  public void process(ResponseBuilder rb) throws IOException {
    // do nothing
  }
	
	@Override
	public void modifyRequest(ResponseBuilder rb, SearchComponent who, ShardRequest sreq) {
	  Set<String> names = sreq.params.getParameterNames();
	  for (String name : names.toArray(new String[names.size()])) {
	    for (String value : sreq.params.getParams(name)) {
	      try {
  	      SolrParams params = QueryParsing.getLocalParams(value, sreq.params);
  	      if (params != null && params.get("type").equals(qParser)) {
  	        sreq.params.remove(name, value);
  	      }
	      } catch (SyntaxError e) {
	        // ignore
	      }
	    }
	  }
	}
	
	@Override
	public String getDescription() {
		return "$description";
	}

	@Override
	public String getSource() {
		return "$source";
	}

}
