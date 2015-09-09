package uk.co.flax.biosolr.pdbe.phmmer;

public class Alignment {
  
  private String pdbIdChain;

  private double eValue;
  
  private float score;
  
  private String species;
  
  public Alignment(String pdbIdChain, double eValue, float score, String species) {
    this.pdbIdChain = pdbIdChain;
    this.eValue = eValue;
    this.score = score;
    this.species = species;
  }

  public String getPdbIdChain() {
    return pdbIdChain;
  }
  
  public double getEValue() {
    return eValue;
  }
  
  public float getScore() {
    return score;
  }
  
  public String getSpecies() {
    return species;
  }
  
}
