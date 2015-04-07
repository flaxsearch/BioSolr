package uk.co.flax.biosolr.pdbe;

public class Alignment {

	// set in constructor
    private double eValue;
    private String pdbId;
    private String chain;
    
    // set by set() methods
    private String querySeq = "";
    private String retSeq = "";
    private String queryOverlapStart = "";
    private String queryOverlapEnd = "";
    private String dbOverlapStart = "";
    private String dbOverlapEnd = "";
    private Double percentIdentity = null;
    
    public Alignment(String pdbId, String chain, double eValue) {
    	this.pdbId = pdbId;
    	this.chain = chain;
    	this.eValue = eValue;
    }
    
    public void addQuerySequence(String q) {
        querySeq += q;
    }

    public void addReturnSequence(String r) {
        retSeq += r;
    }

    public void setQueryOverlapStart(String s) {
        queryOverlapStart = s;
    }

    public void setQueryOverlapEnd(String s) {
        queryOverlapEnd = s;
    }

    public void setDbOverlapStart(String s) {
        dbOverlapStart = s;
    }

    public void setDbOverlapEnd(String s) {
        dbOverlapEnd = s;
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
    
    public String getQuerySequenceString() {
        return querySeq;
    }

    public String getReturnSequenceString() {
        return retSeq;
    }

    public String getQueryOverlapStart() {
        return queryOverlapStart;
    }

    public String getQueryOverlapEnd() {
        return queryOverlapEnd;
    }

    public String getDbOverlapStart() {
        return dbOverlapStart;
    }

    public String getDbOverlapEnd() {
        return dbOverlapEnd;
    }
    
    public String toString() {
    	return retSeq;
    }
}
