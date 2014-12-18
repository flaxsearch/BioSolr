package uk.co.flax.biosolr.pdbe;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private Map<String, List<Alignment>> pdbIdAlignments = new HashMap<>();
    private List<String> resPdbIdCodes = new ArrayList<>();
    private Map<String, Map<String, List<String>>> pdbIdSequenceGroups = new HashMap<>();
    private Map<String, Set<String>> pdbIdChainSelectionGroups = new HashMap<>();
    private Set<String> pdbIdCodes = new HashSet<>();
    
    // regexp patterns
    private Pattern pattern1 = Pattern.compile("^PDB:(.*?_.*?)\\s+(.+?)\\s+([0-9.e-]+?)$|^PRE_PDB:(\\w{4} Entity)\\s+(.+?)\\s+([0-9.e-]+?)$");
    private Pattern pattern2 = Pattern.compile("^>>PDB:(.*?_.*?)\\s+.*?$|^>>PRE_PDB:(\\w{4} Entity).*?$");
    private Pattern pattern3 = Pattern.compile("^Smith-Waterman score:.*?\\;(.*?)\\% .*? overlap \\((.*?)\\)$");
    private Pattern pattern4 = Pattern.compile("^EMBOS  (\\s*.*?)$");            
    private Pattern pattern5 = Pattern.compile("^PDB:.*? (\\s*.*?)$|^PRE_PD.*? (\\s*.*?)$");
    
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
    	return pdbIdAlignments;
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
	            byte[] result = fasta.getResult(jobId, id, null);
	            InputStream in = new ByteArrayInputStream(result);
	        	parseResults(new BufferedReader(new InputStreamReader(in)));
	        } else {
	            LOG.log(Level.SEVERE, "Error with job: " + jobId + " (" + status + ")");
	        }
    	} catch (InterruptedException | IOException e) {
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
    
    private void parseResults(BufferedReader reader) throws IOException {
    	String line = "";
        while (line != null) {
            Matcher matcher1 = pattern1.matcher(line);
            Matcher matcher2 = pattern2.matcher(line);
            if (matcher1.find()) {
                Alignment a = parseAlignment(matcher1);
                
                pdbIdCodes.add(a.getPdbId());
                resPdbIdCodes.add(a.getPdbIdChain());
                alignments.put(a.getPdbIdChain(), a);

                List<Alignment> alignList = pdbIdAlignments.get(a.getPdbId());
                if (alignList == null) {
                	alignList = new ArrayList<>();
                    pdbIdAlignments.put(a.getPdbId(), alignList);
                }
                alignList.add(a);
                
                line = reader.readLine();
            } else if (matcher2.find()) {
            	int n = firstGroup(matcher2);
                String pdbId_chain1 = matcher2.group(n);
                
                if (pdbId_chain1.contains("Entity")) {
                	pdbId_chain1 = pdbId_chain1.replaceFirst(" ", "_");  
                }
                
                Alignment a = alignments.get(pdbId_chain1);
                if (a == null) {
                	continue;
                }

                while ((line = reader.readLine()) != null) {
                    Matcher m2 = pattern2.matcher(line);
                    Matcher m3 = pattern3.matcher(line);
                    Matcher m4 = pattern4.matcher(line);
                    Matcher m5 = pattern5.matcher(line);

                    if (m3.find()) {
                        double identity = new Double(m3.group(1));
                        a.setPercentIdentity(identity);
                        String overLap = m3.group(2);
                        String[] o = overLap.split(":");
                        String[] oIn = o[0].split("-");
                        String[] oOut = o[1].split("-");
                        a.setQueryOverlapStart(oIn[0]);
                        a.setQueryOverlapEnd(oIn[1]);
                        a.setDBOverlapStart(oOut[0]);
                        a.setDBOverlapEnd(oOut[1]);
                    } else if (m2.find()) {
                        break;
                    } else if (m4.find()) {
                        a.addQuerySequence(m4.group(1));
                    } else if (m5.find()) {
                        int n4 = firstGroup(m5);
                        a.addReturnSequence(m5.group(n4));
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
            } else {
            	line = reader.readLine();
            }
        }
        
        for (String pdbId : pdbIdSequenceGroups.keySet()) {
            Set<String> uniqueChains = new HashSet<>();
            Map<String, List<String>> temp = pdbIdSequenceGroups.get(pdbId);
            for (String ss : temp.keySet()) {
            	List<String> chains = temp.get(ss);
                String uChain = null;
                for (String chain : chains) {
                    if (uChain == null) {
                        uChain = chain;
                    } else {
                        Alignment a1 = alignments.get(pdbId + "_" + chain);
                        Alignment a2 = alignments.get(pdbId + "_" + uChain);
                        /*if (a2.getNumAnnotCategories() < a1.getNumAnnotCategories()) {
                            uChain = chain;
                        }*/ //FIXME: doesn't work because there are no annot categories in Alignment
                    }
                }
                assert uChain != null;
                uniqueChains.add(uChain);
                alignmentsToShow.put(pdbId + "_" + uChain, alignments.get(pdbId + "_" + uChain));
            }
            pdbIdChainSelectionGroups.put(pdbId, uniqueChains);
        }
    }
    
}