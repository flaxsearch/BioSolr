package org.apache.solr.search.xjoin;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.search.DocIterator;

import uk.co.flax.biosolr.pdbe.FastaJobResults;

/**
 * SOLR Search Component for performing an "x-join". It must be added to a request handler
 * in both the first and last component lists.
 * 
 * In prepare(), it obtains external process results (based on parameters in the SOLR query
 * URL), and (optionally) places a list of join ids in a query parameter. The join id list
 * should be used as the value to a terms query parser to create the main query or a query
 * filter.
 * 
 * In process(), it appends (selectable) attributes of the external process results to the
 * query results.
 * 
 * Note that results can be sorted or boosted by a property of external results by using
 * the associated XjoinValueSourceParser (creating a custom function which may be referenced
 * in, for example, a sort spec or a boost query).
 */
public class XJoinSearchComponent extends SearchComponent {
	
	/*package*/ static final String RESULTS_TAG = XJoinResults.class.getName();

	// factory for creating XJoinResult objects per search
	private XJoinResultsFactory factory;

	// document field on which to join with external results
	private String joinField;
	
	/**
	 * Initialise the component by instantiating our factory class, and initialising
	 * the join field.
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public void init(NamedList args) {
		super.init(args);
		
		try {
			Class<?> factoryClass = Class.forName((String)args.get(XJoinParameters.INIT_RESULTS_FACTORY));
			factory = (XJoinResultsFactory)factoryClass.newInstance();
			factory.init((NamedList)args.get(XJoinParameters.EXTERNAL_PREFIX));
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		
		joinField = (String)args.get(XJoinParameters.INIT_JOIN_FIELD);
	}
	
	/**
	 * Generate external process results (if they have not already been generated).
	 */
	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
	    SolrParams params = rb.req.getParams();
	    if (! params.getBool(getName(), false)) {
	    	return;
	    }
	    
	    XJoinResults results = (XJoinResults)rb.req.getContext().get(RESULTS_TAG);
	    if (results != null) {
	    	return;
	    }
	    
	    // generate external process results, by passing 'external' prefixed parameters
	    // from the query string to our factory
	    String prefix = getName() + "." + XJoinParameters.EXTERNAL_PREFIX + ".";
	    ModifiableSolrParams externalParams = new ModifiableSolrParams();
	    for (Iterator<String> it = params.getParameterNamesIterator(); it.hasNext(); ) {
	    	String name = it.next();
	    	if (name.startsWith(prefix)) {
	    		externalParams.set(name.substring(prefix.length()), params.get(name));
	    	}
	    }
		results = factory.getResults(externalParams);
		rb.req.getContext().put(XJoinSearchComponent.RESULTS_TAG, results);
		
		String listParameter = (String)params.get(getName() + "." + XJoinParameters.LIST_PARAMETER);
		if (listParameter != null) {
			// put a list of join ids as a request parameter that may be referenced in the URL
			ModifiableSolrParams myParams = new ModifiableSolrParams(rb.req.getParams());
			Iterable<String> joinIds = results.getJoinIds();
			myParams.set(listParameter, String.join(",", joinIds));
			rb.req.setParams(myParams);
		}
	}

	/**
	 * Match up search results and add corresponding data for each result (if we have query
	 * results available).
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public void process(ResponseBuilder rb) throws IOException {
	    SolrParams params = rb.req.getParams();
	    if (! params.getBool(getName(), false)) {
	    	return;
	    }
	    
	    XJoinResults results = (XJoinResults)rb.req.getContext().get(RESULTS_TAG);
	    if (results == null || rb.getResults() == null) {
	    	return;
	    }
	    
	    // general results
	    FieldAppender appender = new FieldAppender((String)params.get(getName() + "." + XJoinParameters.RESULTS_FIELD_LIST, "*"));
	    NamedList general = appender.addNamedList(rb.rsp.getValues(), getName(), results);
	    
    	// per doc results
	    FieldAppender docAppender = new FieldAppender((String)params.get(getName() + "." + XJoinParameters.DOC_FIELD_LIST, "*"));
	    Set<String> joinFields = new HashSet<>();
	    joinFields.add(joinField);
	    for (DocIterator it = rb.getResults().docList.iterator(); it.hasNext(); ) {
	    	Document doc = rb.req.getSearcher().doc(it.nextDoc(), joinFields);
	    	Object object = results.getResult(doc.get(joinField));
	    	if (object != null) {
		    	docAppender.addNamedList(general, "doc", object);
	    	}
	    }
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
