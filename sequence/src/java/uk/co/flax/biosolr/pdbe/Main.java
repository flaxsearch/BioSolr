package uk.co.flax.biosolr.pdbe;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import uk.ac.ebi.webservices.axis1.stubs.fasta.InputParameters;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_PortType;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_Service;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_ServiceLocator;

public class Main {
	
	private static final String EMAIL = "sameer@ebi.ac.uk";
	
	public static void main(String[] args) {
		if (args.length < 4) {
			System.err.println("Too few command line arguments");
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
			
			if (job.getException() != null) {
				System.err.println("Error during run()");
				job.getException().printStackTrace(System.err);;
			}
			
			System.out.println(new String(job.getRawResults()));
		} catch (NumberFormatException e) {
			System.err.println("Cannot parse command line arguments");
			e.printStackTrace(System.err);
		} catch (ServiceException e) {
			System.err.println("Cannot create FASTA service");
			e.printStackTrace(System.err);
		} catch (RemoteException e) {
			System.err.println("Cannot retrieve FASTA results");
			e.printStackTrace(System.err);
		}
	}
	
}