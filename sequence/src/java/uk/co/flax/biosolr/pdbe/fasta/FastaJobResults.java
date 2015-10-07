package uk.co.flax.biosolr.pdbe.fasta;

import java.util.LinkedHashMap;
import java.util.Map;

public class FastaJobResults {

  private Map<PDb.Id, Map<String, PDb.Alignment>> alignments = new LinkedHashMap<>();
  private int numChains = 0;

  /*package*/ void addAlignment(PDb.Alignment alignment) {
    Map<String, PDb.Alignment> map = alignments.get(alignment.getPdbId());
    if (map == null) {
      map = new LinkedHashMap<>();
      alignments.put(alignment.getPdbId(), map);
    }
    if (map.put(alignment.getChain(), alignment) == null) {
      ++numChains;
    }
  }
  
  public PDb.Alignment getAlignment(String pdbId, String chain) {
    Map<String, PDb.Alignment> map = alignments.get(new PDb.Id(pdbId));
    if (map == null) return null;
    return map.get(chain);
  }
  
  public Map<PDb.Id, Map<String, PDb.Alignment>> getAlignments() {
    return alignments;
  }

  public int getNumChains() {
    return numChains;
  }

  public int getNumEntries() {
    return alignments.size();
  }

}
