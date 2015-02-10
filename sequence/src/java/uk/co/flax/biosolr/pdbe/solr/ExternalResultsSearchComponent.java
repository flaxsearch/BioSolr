package uk.co.flax.biosolr.pdbe.solr;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.search.DocIterator;

import uk.co.flax.biosolr.pdbe.FastaJobResults;

/**
 * Extract external results into SOLR results.
 */
public class ExternalResultsSearchComponent extends SearchComponent {

	public static final String INIT_JOIN_FIELD = "joinField";
	public static final String RESULTS_FIELD_LIST = "results";
	public static final String DOC_FIELD_LIST = CommonParams.FL;

	/*package*/ static final String RESULTS_TAG = FastaJobResults.class.getName();
	
	private String joinField;
	
	@Override
	@SuppressWarnings("rawtypes")
	public void init(NamedList args) {
		super.init(args);
		
		joinField = (String)args.get(INIT_JOIN_FIELD);
	}
	
	/**
	 * Nothing to do.
	 */
	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
		// do nothing
	}

	/**
	 * Match up search results and add corresponding alignment data for each result.
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public void process(ResponseBuilder rb) throws IOException {
	    SolrParams params = rb.req.getParams();
	    if (! params.getBool(getName(), false)) {
	    	return;
	    }
	    
	    ExternalResults results = (ExternalResults)rb.req.getContext().get(RESULTS_TAG);
	    if (results == null) {
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
