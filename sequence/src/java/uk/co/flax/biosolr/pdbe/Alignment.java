package uk.co.flax.biosolr.pdbe;

public class Alignment {

  private StringBuilder querySeq = new StringBuilder();
  private StringBuilder retSeq = new StringBuilder();

  // set in constructor
  private double eValue;
  private String pdbId;
  private String chain;

  // set by set() methods
  private Integer queryOverlapStart = null;
  private Integer queryOverlapEnd = null;
  private Integer dbOverlapStart = null;
  private Integer dbOverlapEnd = null;
  private Double percentIdentity = null;

  public Alignment(String pdbId, String chain, double eValue) {
    this.pdbId = pdbId;
    this.chain = chain;
    this.eValue = eValue;
  }

  public void addQuerySequence(String q) {
    querySeq.append(q);
  }

  public String getQuerySequenceString() {
    return querySeq.toString();
  }

  public void setQueryOverlapStart(int n) {
    queryOverlapStart = n;
  }

  public void setQueryOverlapEnd(int n) {
    queryOverlapEnd = n;
  }
  
  public String getQuerySequenceOverlap(String filler) {
    //FIXME this is possibly wrong
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < queryOverlapStart - 1; ++i) {
      s.append(filler);
    }
    s.append(querySeq.substring(queryOverlapStart - 1, queryOverlapEnd));
    for (int i = queryOverlapEnd; i < querySeq.length(); ++i) {
      s.append(filler);
    }
    return s.toString();
  }
  
  public String getQuerySequenceOverlap() {
    return getQuerySequenceOverlap("-");
  }
  
  public void addReturnSequence(String r) {
    retSeq.append(r);
  }

  public String getReturnSequenceString() {
    return retSeq.toString();
  }

  public void setDbOverlapStart(int n) {
    dbOverlapStart = n;
  }

  public void setDbOverlapEnd(int n) {
    dbOverlapEnd = n;
  }
  
  public String getReturnSequenceOverlap(String filler) {
    //FIXME this is possibly wrong
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < dbOverlapStart - 1; ++i) {
      s.append(filler);
    }
    s.append(retSeq.substring(dbOverlapStart - 1, dbOverlapEnd));
    for (int i = dbOverlapEnd; i < retSeq.length(); ++i) {
      s.append(filler);
    }
    return s.toString();
  }

  public String getReturnSequenceOverlap() {
    return getReturnSequenceOverlap("-");
  }
  
  public void setPercentIdentity(double d) {
    percentIdentity = d;
  }

  public double getPercentIdentity() {
    return percentIdentity;
  }

  public double getEValue() {
    return eValue;
  }

  public String getPdbId() {
    return pdbId;
  }

  public String getChain() {
    return chain;
  }

  public String getPdbIdChain() {
    return pdbId + "_" + chain;
  }

  public int getQueryOverlapStart() {
    if (queryOverlapStart == null) {
      throw new RuntimeException("queryOverlapStart not set");
    }
    return queryOverlapStart;
  }

  public int getQueryOverlapEnd() {
    if (queryOverlapEnd == null) {
      throw new RuntimeException("queryOverlapEnd not set");
    }
    return queryOverlapEnd;
  }

  public int getDbOverlapStart() {
    if (dbOverlapStart == null) {
      throw new RuntimeException("dbOverlapStart not set");
    }
    return dbOverlapStart;
  }

  public int getDbOverlapEnd() {
    if (dbOverlapEnd == null) {
      throw new RuntimeException("dbOverlapEnd not set");
    }
    return dbOverlapEnd;
  }

  public String toString() {
    return getPdbIdChain();
  }
}
