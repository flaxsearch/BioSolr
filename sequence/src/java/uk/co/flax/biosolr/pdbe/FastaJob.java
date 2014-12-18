package uk.co.flax.biosolr.pdbe;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ebi.webservices.axis1.stubs.fasta.InputParameters;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_PortType;

public class FastaJob implements Runnable {
	
	private static final Logger LOG  = Logger.getLogger(FastaJob.class.getName());

    private static final String EMAIL = "sameer@ebi.ac.uk";

    // inputs
    private JDispatcherService_PortType fasta;
    private InputParameters params = new InputParameters();
    private float identityPctL = 0.0f;
    private float identityPctH = 100.0f;
    
    // exception caught during run(), if any, and the run status
    private Exception exception;
    private String status;

    // outputs
    private Map<String, Alignment> alignments = new HashMap<>();
    private Map<String, Alignment> alignmentsToShow = new HashMap<>();
    private Map<String, List<Alignment>> alignPdbIdCode = new HashMap<>();
    private List<String> resPdbIdCodes = new ArrayList<>();
    private Map<String, Map<String, List<String>>> pdbIdSequenceGroups = new HashMap<>();
    private Map<String, Set<String>> pdbIdChainSelectionGroups = new HashMap<>();
    private Set<String> pdbIdCodes = new HashSet<>();
    
    public Exception getException() {
    	return exception;
    }
    
    public String getStatus() {
    	return status;
    }
    
    public Map<String, Alignment> getAlignments() {
    	return alignmentsToShow;
    }

    public List<String> getResultOrder() {
    	return resPdbIdCodes;
    }
    
    public String getPdbIdCodes() {
    	return String.join(",", pdbIdCodes);
    }

    public Map<String, List<Alignment>> getAlignPdbIdCodes() {
    	return alignPdbIdCode;
    }
    
    public String getQuerySequence() {
    	return params.getSequence();
    }
    
    public double getEValue() {
    	return params.getExpupperlim();
    }
    
    public float getIdentityPctL() {
    	return identityPctL;
    }
    
    public float getIdentityPctH() {
    	return identityPctH;
    }
    
    public int getNumChains() {
    	return resPdbIdCodes.size();
    }
    
    public int getNumEntries() {
    	return pdbIdCodes.size();
    }
    
    public Map<String, Set<String>> getAlignChains() {
    	return pdbIdChainSelectionGroups;
    }
    
    public FastaJob(JDispatcherService_PortType fasta, String sequence, double eValueCutoff) {
    	this.fasta = fasta;
        params.setProgram("ssearch");
        params.setDatabase(new String[] { "pdb" });
        params.setExpupperlim(eValueCutoff);
		params.setExplowlim(0.0d);
        params.setScores(1000);
        params.setAlignments(1000);
        params.setStype("protein");
        params.setSequence(sequence);
	}
    
    public void setIdentityPercents(float identityPctL, float identityPctH) {
    	this.identityPctL = identityPctL;
    	this.identityPctH = identityPctH;
    }

    private int firstGroup(Matcher m) {
    	for (int n = 1; n <= m.groupCount(); ++n) {
    		if (m.group(n) != null) {
    			return n;
    		}
    	}
    	return 0;
    }
    
    public void run() {
    	try {
	        String jobId = fasta.run(EMAIL, "", params);
	
	        do {
	            Thread.sleep(200);
	            status = fasta.getStatus(jobId);
	            LOG.log(Level.FINE, status);
	        } while (status.equals(FastaStatus.RUNNING));
	        
	        if (status.equals(FastaStatus.DONE)) {
	        	String id = fasta.getResultTypes(jobId)[0].getIdentifier();
	            String result = new String(fasta.getResult(jobId, id, null));
	        	parseResults(result);
	        } else {
	            LOG.log(Level.SEVERE, "Error with job: " + jobId + " (" + status + ")");
	        }
    	} catch (RemoteException | InterruptedException e) {
    		exception = e;
    	}
    }
    
    // create an Alignment from a matching line
    private Alignment parseAlignment(Matcher matcher) {
    	int n = firstGroup(matcher);
    	String pdbIdChain = matcher.group(n);
        if (pdbIdChain.contains("Entity")) {
        	pdbIdChain = pdbIdChain.replaceFirst(" ", "_");
        }
        String[] s = pdbIdChain.split("_");
        String pdbId = s[0];
        String chain = s[1];
        double eValue = new Double(matcher.group(n + 2));
        return new Alignment(pdbId, chain, eValue);
    }
    
    private void parseResults(String result) throws RemoteException {
        String[] output = result.split("\n");

        Pattern pattern = Pattern.compile("^PDB:(.*?_.*?)\\s+(.+?)\\s+([0-9.e-]+?)$|^PRE_PDB:(\\w{4} Entity)\\s+(.+?)\\s+([0-9.e-]+?)$");
        Pattern pattern1 = Pattern.compile("^>>PDB:(.*?_.*?)\\s+.*?$|^>>PRE_PDB:(\\w{4} Entity).*?$");
        Pattern pattern2 = Pattern.compile("^Smith-Waterman score:.*?\\;(.*?)\\% .*? overlap \\((.*?)\\)$");
        Pattern pattern3 = Pattern.compile("^EMBOS  (\\s*.*?)$");            
        Pattern pattern4 = Pattern.compile("^PDB:.*? (\\s*.*?)$|^PRE_PD.*? (\\s*.*?)$");

        for (int i = 1; i < output.length; i++) {
            Matcher matcher = pattern.matcher(output[i]);
            Matcher matcher1 = pattern1.matcher(output[i]);
            if (matcher.find()) {
                Alignment a = parseAlignment(matcher);
                
                pdbIdCodes.add(a.getPdbId());
                resPdbIdCodes.add(a.getPdbIdChain());
                alignments.put(a.getPdbIdChain(), a);

                List<Alignment> v = alignPdbIdCode.get(a.getPdbId());
                if (v == null) {
                	v = new ArrayList<>();
                    alignPdbIdCode.put(a.getPdbId(), v);
                }
                v.add(a);
            } else if (matcher1.find()) {
            	int n = firstGroup(matcher1);
                String pdbId_chain1 = matcher1.group(n);
                
                if (pdbId_chain1.contains("Entity")) {
                	pdbId_chain1 = pdbId_chain1.replaceFirst(" ", "_");  
                }
                
                Alignment a = alignments.get(pdbId_chain1);
                if (a == null) {
                	continue;
                }

                for (int j = i + 1; j < output.length; j++) {
                    Matcher m1 = pattern1.matcher(output[j]);
                    Matcher m2 = pattern2.matcher(output[j]);
                    Matcher m3 = pattern3.matcher(output[j]);
                    Matcher m4 = pattern4.matcher(output[j]);

                    if (m2.find()) {
                        double identity = new Double(m2.group(1));
                        a.setPercentIdentity(identity);
                        String overLap = m2.group(2);
                        String[] o = overLap.split(":");
                        String[] oIn = o[0].split("-");
                        String[] oOut = o[1].split("-");
                        a.setQueryOverlapStart(oIn[0]);
                        a.setQueryOverlapEnd(oIn[1]);
                        a.setDBOverlapStart(oOut[0]);
                        a.setDBOverlapEnd(oOut[1]);
                    } else if (m1.find()) {
                        i = j - 1;
                        break;
                    } else if (m3.find()) {
                        a.addQuerySequence(m3.group(1));
                    } else if (m4.find()) {
                        int n4 = firstGroup(m4);
                        a.addReturnSequence(m4.group(n4));
                    }
                }
                
                Map<String, List<String>> seq = pdbIdSequenceGroups.get(a.getPdbId());
                if (seq == null) {
                	seq = new HashMap<>();
                    pdbIdSequenceGroups.put(a.getPdbId(), seq);
                }
                List<String> chains = seq.get(a.getReturnSequenceString());
                if (chains == null) {
                    chains = new ArrayList<>();
                    seq.put(a.getReturnSequenceString(), chains);
                }
                chains.add(a.getChain());
            }
        }
        
        for (String pdbCode : pdbIdSequenceGroups.keySet()) {
            Set<String> uniqueChains = new HashSet<>();
            Map<String, List<String>> temp = pdbIdSequenceGroups.get(pdbCode);
            for (String ss : temp.keySet()) {
            	List<String> chains = temp.get(ss);
                String uChain = null;
                for (String chain : chains) {
                    if (uChain == null) {
                        uChain = chain;
                    } else {
                        Alignment a1 = alignments.get(pdbCode + "_" + chain);
                        Alignment a2 = alignments.get(pdbCode + "_" + uChain);
                        /*if (a2.getNumAnnotCategories() < a1.getNumAnnotCategories()) {
                            uChain = chain;
                        }*/ //FIXME: doesn't work because there are no annot categories in Alignment
                    }
                }
                assert uChain != null;
                uniqueChains.add(uChain);
                alignmentsToShow.put(pdbCode + "_" + uChain, alignments.get(pdbCode + "_" + uChain));
            }
            pdbIdChainSelectionGroups.put(pdbCode, uniqueChains);
        }
    }
}