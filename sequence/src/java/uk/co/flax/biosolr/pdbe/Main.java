package uk.co.flax.biosolr.pdbe;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.rpc.ServiceException;

import uk.ac.ebi.webservices.axis1.stubs.fasta.InputParameters;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_PortType;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_Service;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_ServiceLocator;

public class Main {
	
	private static final Logger LOG  = Logger.getLogger(Main.class.getName());
	
	private static final String EMAIL = "sameer@ebi.ac.uk";
	
	public static void main(String[] args) {
		if (args.length < 4) {
			LOG.log(Level.SEVERE, "Too few command line arguments");
			return;
		}
		try {
	        JDispatcherService_Service service = new JDispatcherService_ServiceLocator();
	        JDispatcherService_PortType fasta = service.getJDispatcherServiceHttpPort();

			String sequence = args[0];
			double eVal = new Double(args[1]);
			
			InputParameters params = new InputParameters();
			params.setProgram("ssearch");
			params.setDatabase(new String[] { "pdb" });
			params.setStype("protein");
	        params.setSequence(sequence);
	        params.setExplowlim(0.0d);
	        params.setExpupperlim(eVal);
	        params.setScores(1000);
	        params.setAlignments(1000);
			
	        FastaJob job = new FastaJob(fasta, EMAIL, params);
			job.run();
			FastaJobResults results = job.getResults();
			LOG.log(Level.INFO, "Number of chains: " + results.getNumChains());
			LOG.log(Level.INFO, "Number of entries: " + results.getNumEntries());
			
			if (job.getException() != null) {
				LOG.log(Level.SEVERE, "Error during run()", job.getException());
			}
		} catch (NumberFormatException e) {
			LOG.log(Level.SEVERE, "Cannot parse command line arguments", e);
		} catch (ServiceException e) {
			LOG.log(Level.SEVERE, "Cannot create FASTA service", e);
		}
	}
	
}