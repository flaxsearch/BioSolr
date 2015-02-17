package uk.co.flax.biosolr.pdbe.solr;

import java.io.IOException;
import java.util.Iterator;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;

/**
 * 
 */
public class ExternalSearchComponent extends SearchComponent {

	public static final String INIT_RESULTS_FACTORY = "factoryClass";
	public static final String LIST_PARAMETER = "listParameter";
	public static final String EXTERNAL_PREFIX = "external";

	private ExternalResultsFactory factory;
	
	@Override
	@SuppressWarnings("rawtypes")
	public void init(NamedList args) {
		super.init(args);
		
		try {
			Class<?> factoryClass = Class.forName((String)args.get(INIT_RESULTS_FACTORY));
			factory = (ExternalResultsFactory)factoryClass.newInstance();
			factory.init((NamedList)args.get(EXTERNAL_PREFIX));
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 
	 */
	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
	    SolrParams params = rb.req.getParams();
	    if (! params.getBool(getName(), false)) {
	    	return;
	    }
	    
	    String prefix = getName() + "." + params.get(getName() + "." + EXTERNAL_PREFIX + ".");
	    ModifiableSolrParams externalParams = new ModifiableSolrParams();
	    for (Iterator<String> it = params.getParameterNamesIterator(); it.hasNext(); ) {
	    	String name = it.next();
	    	if (name.startsWith(prefix)) {
	    		externalParams.set(name.substring(prefix.length()), params.get(name));
	    	}
	    }
		ExternalResults results = factory.getResults(externalParams);
		rb.req.getContext().put(ExternalResultsSearchComponent.RESULTS_TAG, results);
		
		String listParameter = (String)params.get(getName() + "." + LIST_PARAMETER);
		if (listParameter != null) {
			// put a list of join ids as a request parameter that may be referenced in the URL
			ModifiableSolrParams myParams = new ModifiableSolrParams(rb.req.getParams());
			myParams.set(listParameter, results.getJoinList());
			rb.req.setParams(myParams);
		}
	}

	/**
	 * Nothing to do.
	 */
	@Override
	public void process(ResponseBuilder rb) throws IOException {
	    // do nothing
	}

	@Override
	public String getDescription() {
		return "$description$";
	}

	@Override
	public String getSource() {
		return "$source$";
	}

}
