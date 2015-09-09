package uk.co.flax.biosolr.pdbe.phmmer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class PhmmerJob {

  // phmmer REST service URL
  public static final String BASE_URL = "http://www.ebi.ac.uk/Tools/hmmer/search/phmmer";

  private String database;
  
  public PhmmerJob(String database) {
    this.database = database;
  }

  public PhmmerResults getResults(String sequence) throws IOException {
    String respUrl = getResultsUrl(sequence);
    JsonObject response = getResultsJson(respUrl);
    
    JsonObject results = response.getJsonObject("results");
    JsonArray hits = results.getJsonArray("hits");

    PhmmerResults phmmer = new PhmmerResults(hits.size());
    for (int i = 0; i < hits.size(); ++i) {
      JsonObject hit = hits.getJsonObject(i);
      String pdbIdChain = hit.getString("acc");
      double eValue = Double.parseDouble(hit.getString("evalue"));
      Alignment a = new Alignment(pdbIdChain, eValue);
      phmmer.addAlignment(a);
    }
    return phmmer;
  }
  
  private String getResultsUrl(String sequence) throws IOException {
    try (HttpConnection http = new HttpConnection(BASE_URL)) {
      http.post("seqdb=" + database + "&" + "seq=>" + "Seq\n" + sequence);
      return http.getHeader("Location");
    }
  }

  private JsonObject getResultsJson(String url) throws IOException {
    try (HttpConnection http = new HttpConnection(url)) {
      http.get("application/json");
      return http.getJson();
    }
  }
  
  private class HttpConnection implements AutoCloseable {

    private HttpURLConnection http;
    
    private HttpConnection(String url) throws IOException {
      http = (HttpURLConnection)new URL(url).openConnection();
    }
    
    private void post(String params) throws IOException {
      http.setRequestMethod("POST");
      http.setDoOutput(true);
      http.setInstanceFollowRedirects(false);
      http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
      http.setRequestProperty("Content-Length", Integer.toString(params.getBytes().length));
      try (OutputStream out = http.getOutputStream()) {
        out.write(params.getBytes());
      }
    }
    
    private String getHeader(String key) {
      return http.getHeaderField(key);
    }
    
    private void get(String accept) throws ProtocolException {
      http.setRequestMethod("GET");
      http.setRequestProperty("Accept", accept);
    }
    
    private JsonObject getJson() throws IOException {
      try (InputStream in = http.getInputStream();
           JsonReader reader = Json.createReader(in)) {
        return reader.readObject();
      }
    }
    
    @Override
    public void close() {
      http.disconnect();
    }
    
  }
  
}
