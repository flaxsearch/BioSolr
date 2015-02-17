package uk.co.flax.biosolr.pdbe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.co.flax.biosolr.pdbe.solr.ExternalResults;

public class FastaJobResults implements ExternalResults {
    
    private Map<String, Alignment> alignments = new HashMap<>();
    private Map<String, Alignment> alignmentsToShow = new HashMap<>();
    private Map<String, List<Alignment>> pdbIdAlignments = new HashMap<>();
    private List<String> pdbIdChains = new ArrayList<>();
    private Map<String, Map<String, List<String>>> pdbIdSequenceGroups = new HashMap<>();
    private Map<String, Set<String>> pdbIdChainSelectionGroups = new HashMap<>();
    
    /*package*/ void addAlignment(Alignment alignment) {
        pdbIdChains.add(alignment.getPdbIdChain());
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
    
    public Alignment getAlignment(String pdbIdChain) {
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
                        }*/ //FIXME: doesn't work because there are no annotation categories in Alignment
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
    	return pdbIdChains;
    }
    
    public String getPdbIdCodes() {
    	return String.join(",", pdbIdAlignments.keySet());
    }
    
    public String getJoinList() {
    	String[] entries = new String[alignmentsToShow.size()];
    	int i = 0;
    	for (Alignment a : alignmentsToShow.values()) {
    		entries[i++] = a.getEntryEntity();
    	}
    	return String.join(",", entries);
    }
    
    public Alignment getResult(String entryEntity) {
    	String[] bits = entryEntity.split("_");
    	if (bits.length != 2) {
    		throw new RuntimeException("Bad entry_entity format: " + entryEntity);
    	}
    	String pdbId = bits[0].toUpperCase();
    	int chain = Integer.parseInt(bits[1]) + 'A' - 1;
    	String pdbIdChain = pdbId + "_" + Character.valueOf((char)chain);
    	return alignments.get(pdbIdChain);
    }

    public Map<String, List<Alignment>> getAlignPdbIdCodes() {
    	return pdbIdAlignments;
    }
    
    public int getNumChains() {
    	return pdbIdChains.size();
    }
    
    public int getNumEntries() {
    	return pdbIdAlignments.size();
    }
    
    public Map<String, Set<String>> getAlignChains() {
    	return pdbIdChainSelectionGroups;
    }

}
