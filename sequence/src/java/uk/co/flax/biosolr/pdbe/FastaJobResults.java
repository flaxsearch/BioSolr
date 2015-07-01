package uk.co.flax.biosolr.pdbe;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class FastaJobResults {

  private Map<String, Alignment> alignments = new LinkedHashMap<>();
  private Map<String, Alignment> alignmentsToShow = new LinkedHashMap<>();
  private Set<String> pdbIds = new HashSet<>();

  /*package*/ void addAlignment(Alignment alignment) {
    if (alignments.containsKey(alignment.getPdbIdChain())) {
      // ignore repeat entries in the results
      return;
    }

    alignments.put(alignment.getPdbIdChain(), alignment);

    /*
     * List<Alignment> alignList = pdbIdAlignments.get(alignment.getPdbId()); if
     * (alignList == null) { alignList = new ArrayList<>();
     * pdbIdAlignments.put(alignment.getPdbId(), alignList); }
     * alignList.add(alignment);
     */
  }

  public Alignment getAlignment(String pdbIdChain) {
    return alignments.get(pdbIdChain);
  }

  /*package*/ void chooseShownAlignments() {
    // alignments are already ordered by increasing e-value, so just use the
    // first
    // occuring alignment with a particular pdb id
    for (String pdbIdChain : alignments.keySet()) {
      Alignment alignment = alignments.get(pdbIdChain);
      String pdbId = alignment.getPdbId();
      if (pdbIds.contains(pdbId)) {
        continue;
      }
      alignmentsToShow.put(pdbIdChain, alignment);
      pdbIds.add(pdbId);
    }
  }

  public Map<String, Alignment> getAlignments(boolean uniquePdbIds) {
    return uniquePdbIds ? alignmentsToShow : alignments;
  }

  public Set<String> getPdbIds() {
    return pdbIds;
  }

  public int getNumChains() {
    return alignments.size();
  }

  public int getNumEntries() {
    return pdbIds.size();
  }

}
