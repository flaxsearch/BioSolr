package uk.co.flax.biosolr.pdbe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FastaJobResults {

    private String querySequence;
    private double eValue;
    private float identityPctL;
    private float identityPctH;
    
    public FastaJobResults(String querySequence, double eValue, float identityPctL, float identityPctH) {
    	this.querySequence = querySequence;
    	this.eValue = eValue;
    	this.identityPctL = identityPctL;
    	this.identityPctH = identityPctH;
    }
    
    private Map<String, Alignment> alignments = new HashMap<>();
    private Map<String, Alignment> alignmentsToShow = new HashMap<>();
    private Map<String, List<Alignment>> pdbIdAlignments = new HashMap<>();
    private List<String> resPdbIdCodes = new ArrayList<>();
    private Map<String, Map<String, List<String>>> pdbIdSequenceGroups = new HashMap<>();
    private Map<String, Set<String>> pdbIdChainSelectionGroups = new HashMap<>();
    private Set<String> pdbIdCodes = new HashSet<>();
    
    /*package*/ void addAlignment(Alignment alignment) {
        pdbIdCodes.add(alignment.getPdbId());
        resPdbIdCodes.add(alignment.getPdbIdChain());
        alignments.put(alignment.getPdbIdChain(), alignment);

        List<Alignment> alignList = pdbIdAlignments.get(alignment.getPdbId());
        if (alignList == null) {
        	alignList = new ArrayList<>();
        	pdbIdAlignments.put(alignment.getPdbId(), alignList);
        }
        alignList.add(alignment);
    }
    
    /*package*/ void updateSequenceGroup(Alignment alignment) {
        Map<String, List<String>> seq = pdbIdSequenceGroups.get(alignment.getPdbId());
        if (seq == null) {
        	seq = new HashMap<>();
        	pdbIdSequenceGroups.put(alignment.getPdbId(), seq);
        }
        List<String> chains = seq.get(alignment.getReturnSequenceString());
        if (chains == null) {
            chains = new ArrayList<>();
            seq.put(alignment.getReturnSequenceString(), chains);
        }
        chains.add(alignment.getChain());
    }
    
    /*package*/ Alignment getAlignment(String pdbIdChain) {
    	return alignments.get(pdbIdChain);
    }
    
    /*package*/ void chooseShownAlignments() {
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
    	return querySequence;
    }
    
    public double getEValue() {
    	return eValue;
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

}
