package uk.co.flax.biosolr.pdbe.phmmer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PhmmerResults {

  private Map<String, Alignment> alignments;
  
  public PhmmerResults(int size) {
    alignments = new HashMap<>(size);
  }
  
  public void addAlignment(Alignment alignment) {
    alignments.put(alignment.getTarget(), alignment);
  }
  
  public Set<String> getTargets() {
    return alignments.keySet();
  }
  
  public int getSize() {
    return alignments.size();
  }
  
  public Alignment getAlignment(String pdbIdChain) {
    return alignments.get(pdbIdChain);
  }
  
}
