package uk.co.flax.biosolr.pdbe.phmmer;

public class Alignment {
  
  private String pdbIdChain;

  private double eValue;
  
  public Alignment(String pdbIdChain, double eValue) {
    this.pdbIdChain = pdbIdChain;
    this.eValue = eValue;
  }

  public String getPdbIdChain() {
    return pdbIdChain;
  }
  
  public double getEValue() {
    return eValue;
  }
  
}
