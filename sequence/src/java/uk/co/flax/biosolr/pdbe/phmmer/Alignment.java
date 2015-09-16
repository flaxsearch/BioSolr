package uk.co.flax.biosolr.pdbe.phmmer;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class Alignment {
  
  public static double SIGNIFICANCE_THRESHOLD = 1.0d;
  
  private String pdbIdChain;
  
  private String species;
  
  private String description;
  
  private double score;

  private double eValue;

  private double eValueInd;

  private double eValueCond;

  private String querySequence;
  
  private int querySequenceStart;
  
  private int querySequenceEnd;
  
  private String match;
  
  private String targetSequence;
  
  private int targetSequenceStart;
  
  private int targetSequenceEnd;
  
  private int targetEnvelopeStart;
  
  private int targetEnvelopeEnd;
  
  private String posteriorProbability;
  
  private double bias;
  
  private double accuracy;
  
  private double bitScore;
  
  private double identityPercent;
  
  private int identityCount;
  
  private double similarityPercent;
  
  private int similarityCount;
  
  public Alignment(JsonObject hit) {
    pdbIdChain = hit.getString("acc");
    species = hit.getString("species");
    description = hit.getString("desc");
    score = Double.parseDouble(hit.getString("score"));
    eValue = Double.parseDouble(hit.getString("evalue"));

    JsonArray domains = hit.getJsonArray("domains");
    for (int i = 0; i < domains.size(); ++i) {
      JsonObject domain = domains.getJsonObject(i);
      
      // skip insignificant matches (by ind. eValue)
      eValueInd = Double.parseDouble(domain.getString("ievalue"));
      if (eValueInd >= SIGNIFICANCE_THRESHOLD) continue;
      
      eValueCond = Double.parseDouble(domain.getString("cevalue"));
      
      querySequence = domain.getString("alimodel");
      querySequenceStart = domain.getInt("alihmmfrom");
      querySequenceEnd = domain.getInt("alihmmto");
      
      match = domain.getString("alimline");
      
      targetSequence = domain.getString("aliaseq");
      targetSequenceStart = domain.getInt("alisqfrom");
      targetSequenceEnd = domain.getInt("alisqto");
      
      targetEnvelopeStart = domain.getInt("ienv");
      targetEnvelopeEnd = domain.getInt("jenv");
      
      posteriorProbability = domain.getString("alippline");
      
      bias = Double.parseDouble(domain.getString("bias"));
      accuracy = Double.parseDouble(domain.getString("oasc"));
      bitScore = domain.getJsonNumber("bitscore").doubleValue();
      
      identityPercent = 100 * domain.getJsonNumber("aliId").doubleValue();
      identityCount = domain.getInt("aliIdCount");
      
      similarityPercent = 100 * domain.getJsonNumber("aliSim").doubleValue();
      similarityCount = domain.getInt("aliSimCount");
      
      // we consider only the first significant match
      break;
    }
  }

  public String getPdbIdChain() {
    return pdbIdChain;
  }
  
  public double getEValue() {
    return eValue;
  }

  public double getEValueInd() {
    return eValueInd;
  }

  public double getEValueCond() {
    return eValueCond;
  }
  
  public double getBitScore() {
    return bitScore;
  }
  
  public String getSpecies() {
    return species;
  }
  
  public String getDescription() {
    return description;
  }

  public double getScore() {
    return score;
  }

  public String getQuerySequence() {
    return querySequence;
  }

  public int getQuerySequenceStart() {
    return querySequenceStart;
  }

  public int getQuerySequenceEnd() {
    return querySequenceEnd;
  }

  public String getMatch() {
    return match;
  }

  public String getTargetSequence() {
    return targetSequence;
  }

  public int getTargetSequenceStart() {
    return targetSequenceStart;
  }

  public int getTargetSequenceEnd() {
    return targetSequenceEnd;
  }

  public int getTargetEnvelopeStart() {
    return targetEnvelopeStart;
  }

  public int getTargetEnvelopeEnd() {
    return targetEnvelopeEnd;
  }

  public String getPosteriorProbability() {
    return posteriorProbability;
  }

  public double getBias() {
    return bias;
  }

  public double getAccuracy() {
    return accuracy;
  }

  public double getIdentityPercent() {
    return identityPercent;
  }

  public int getIdentityCount() {
    return identityCount;
  }

  public double getSimilarityPercent() {
    return similarityPercent;
  }

  public int getSimilarityCount() {
    return similarityCount;
  }

}
