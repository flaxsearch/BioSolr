package uk.co.flax.biosolr.pdbe.solr;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.search.DocIterator;

import uk.co.flax.biosolr.pdbe.FastaJobResults;

/**
 * 
 */
public class XJoinSearchComponent extends SearchComponent {

	public static final String INIT_RESULTS_FACTORY = "factoryClass";
	public static final String INIT_JOIN_FIELD = "joinField";
	public static final String LIST_PARAMETER = "listParameter";
	public static final String EXTERNAL_PREFIX = "external";
	public static final String RESULTS_FIELD_LIST = "results";
	public static final String DOC_FIELD_LIST = CommonParams.FL;
	/*package*/ static final String RESULTS_TAG = FastaJobResults.class.getName();

	private XJoinResultsFactory factory;
	
	private String joinField;
	
	@Override
	@SuppressWarnings("rawtypes")
	public void init(NamedList args) {
		super.init(args);
		
		try {
			Class<?> factoryClass = Class.forName((String)args.get(INIT_RESULTS_FACTORY));
			factory = (XJoinResultsFactory)factoryClass.newInstance();
			factory.init((NamedList)args.get(EXTERNAL_PREFIX));
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		
		joinField = (String)args.get(INIT_JOIN_FIELD);
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
	    
	    XJoinResults results = (XJoinResults)rb.req.getContext().get(RESULTS_TAG);
	    if (results != null) {
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
		results = factory.getResults(externalParams);
		rb.req.getContext().put(XJoinSearchComponent.RESULTS_TAG, results);
		
		String listParameter = (String)params.get(getName() + "." + LIST_PARAMETER);
		if (listParameter != null) {
			// put a list of join ids as a request parameter that may be referenced in the URL
			ModifiableSolrParams myParams = new ModifiableSolrParams(rb.req.getParams());
			myParams.set(listParameter, results.getJoinList());
			rb.req.setParams(myParams);
		}
	}

	/**
	 * Match up search results and add corresponding data for each result.
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
	    FieldAppender appender = new FieldAppender((String)params.get(getName() + "." + RESULTS_FIELD_LIST, "*"));
	    NamedList general = appender.addNamedList(rb.rsp.getValues(), getName(), results);
	    
    	// per doc results
	    FieldAppender docAppender = new FieldAppender((String)params.get(getName() + "." + DOC_FIELD_LIST, "*"));
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
