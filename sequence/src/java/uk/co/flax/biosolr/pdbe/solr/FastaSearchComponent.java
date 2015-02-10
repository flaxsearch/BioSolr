package uk.co.flax.biosolr.pdbe.solr;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.rpc.ServiceException;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;

import uk.ac.ebi.webservices.axis1.stubs.fasta.InputParameters;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_PortType;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_Service;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_ServiceLocator;
import uk.ac.ebi.webservices.axis1.stubs.fasta.WsResultType;
import uk.co.flax.biosolr.pdbe.FastaJob;
import uk.co.flax.biosolr.pdbe.FastaJobResults;
import uk.co.flax.biosolr.pdbe.FastaStatus;

/**
 * Connect to FASTA service and generate a PDB id filter based on a user supplied
 * sequence.
 * 
 * program = ssearch
 * database = pdb
 * stype = protein
 * 
 * explowlim = 0.0d
 * scores = 1000
 * alignments = 1000
 */
public class FastaSearchComponent extends SearchComponent {

	public static final String COMPONENT_NAME = "fasta_search";
	
	// component initialisation parameters
	public static final String INIT_EMAIL = "email";
	public static final String INIT_PROGRAM = "program";
	public static final String INIT_DATABASE = "database";
	public static final String INIT_STYPE = "stype";
	public static final String INIT_DEBUG_FILE = "debug.file";
	
	// request handler parameters
	public static final String FASTA_EXPLOWLIM = COMPONENT_NAME + ".explowlim";
	public static final String FASTA_EXPUPPERLIM = COMPONENT_NAME + ".expupperlim";
	public static final String FASTA_SEQUENCE = COMPONENT_NAME + ".sequence";
	public static final String FASTA_SCORES = COMPONENT_NAME + ".scores";
	public static final String FASTA_ALIGNMENTS = COMPONENT_NAME + ".alignments";
	public static final String FASTA_LIST_PARAMETER = COMPONENT_NAME + ".listParameter";
	
	private JDispatcherService_PortType fasta;
	private String email;
    private InputParameters params;
    private String debugFile;

	@Override
	@SuppressWarnings("rawtypes")
	public void init(NamedList args) {
		super.init(args);
		
		debugFile = (String)args.get(INIT_DEBUG_FILE);
		if (debugFile != null) {
			try {
				byte[] result = Files.readAllBytes(Paths.get(debugFile));
				fasta = mock(JDispatcherService_PortType.class);
				when(fasta.getStatus(null)).thenReturn(FastaStatus.DONE);
				WsResultType[] types = new WsResultType[] { mock(WsResultType.class) };
				when(fasta.getResultTypes(null)).thenReturn(types);
				when(fasta.getResult(null, null, null)).thenReturn(result);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
	        JDispatcherService_Service service = new JDispatcherService_ServiceLocator();
	        try {
				fasta = service.getJDispatcherServiceHttpPort();
			} catch (ServiceException e) {
				throw new RuntimeException(e);
			}
		}
		
        email = (String)args.get(INIT_EMAIL);
        params = new InputParameters();
        params.setProgram((String)args.get(INIT_PROGRAM));
        params.setDatabase(new String[] { (String)args.get(INIT_DATABASE) });
        params.setStype((String)args.get(INIT_STYPE));
	}
	
	private String getParam(SolrParams params, String name) {
	    String value = params.get(name);
	    if (value == null || value.length() == 0) {
	    	throw new RuntimeException("Missing or empty " + name);
	    }
		return value;
	}
	
	/**
	 * Call out to the FASTA service and add a filter query based on the response.
	 */
	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
	    SolrParams params = rb.req.getParams();
	    if (! params.getBool(COMPONENT_NAME, false)) {
	    	return;
	    }
	    
	    if (debugFile == null) {
	        this.params.setSequence(getParam(params, FASTA_SEQUENCE));
	        this.params.setExplowlim(new Double(getParam(params, FASTA_EXPLOWLIM)));
	        this.params.setExpupperlim(new Double(getParam(params, FASTA_EXPUPPERLIM)));
	        this.params.setScores(new Integer(getParam(params, FASTA_SCORES)));
	        this.params.setAlignments(new Integer(getParam(params, FASTA_ALIGNMENTS)));
	    }
	    
        FastaJob job = new FastaJob(fasta, email, this.params);
		job.run();
		
		if (! job.resultsOk()) {
			if (job.getException() != null) {
				throw new RuntimeException(job.getException());
			}
			if (! FastaStatus.DONE.equals(job.getStatus())) {
				throw new RuntimeException("Unexpected FASTA job status: " + job.getStatus());
			}
			if (job.isInterrupted()) {
				throw new RuntimeException("FASTA job was interrupted");
			}
			throw new RuntimeException("No results");
		}
		
		rb.req.getContext().put(ExternalResultsSearchComponent.RESULTS_TAG, job.getResults());
		
		String listParameter = (String)params.get(FASTA_LIST_PARAMETER);
		if (listParameter != null) {
			ModifiableSolrParams myParams = new ModifiableSolrParams(rb.req.getParams());
			myParams.set(listParameter, job.getResults().getEntryEntityCodes());
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
		return "FASTA search component";
	}

	@Override
	public String getSource() {
		return "$source$";
	}

}
