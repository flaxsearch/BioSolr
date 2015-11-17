package uk.co.flax.biosolr.pdbe.phmmer;

import java.io.IOException;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class PhmmerJob {
  
  private PhmmerClient client;

  private String database;
  
  private String sequence;
  
  public PhmmerJob(PhmmerClient client, String database, String sequence) {
    this.database = database;
    this.sequence = sequence;
    this.client = client;
  }

  public PhmmerResults runJob() throws IOException {
    JsonObject response = client.getResults(database, sequence);
    JsonObject results = response.getJsonObject("results");
    JsonArray hits = results.getJsonArray("hits");

    PhmmerResults phmmer = new PhmmerResults(hits.size());
    for (int i = 0; i < hits.size(); ++i) {
      Alignment a = new Alignment(hits.getJsonObject(i));
      phmmer.addAlignment(a);
    }
    return phmmer;
  }
  
}
