package uk.co.flax.biosolr.pdbe.phmmer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PhmmerResults {

  private Map<String, Map<String, Alignment>> alignments;
  
  private int numChains;
  
  public PhmmerResults(int size) {
    alignments = new HashMap<>(size);
    numChains = 0;
  }
  
  /*package*/ void addAlignment(Alignment alignment) {
    String[] bits = alignment.getTarget().split("_"); // split out the pdb id from e.g. 1cms_A
    Map<String, Alignment> map = alignments.get(bits[0]);
    if (map == null) {
      map = new HashMap<>();
      alignments.put(bits[0], map);
    }
    if (map.put(bits[1], alignment) == null) {
      ++numChains;
    }
  }
  
  public Set<String> getPdbIds() {
    return alignments.keySet();
  }
  
  public Map<String, Map<String, Alignment>> getAlignments() {
    return alignments;
  }

  public int getNumChains() {
    return numChains;
  }

  public int getNumEntries() {
    return alignments.size();
  }
  
}
