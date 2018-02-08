package uk.co.flax.biosolr.pdbe.phmmer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import uk.co.flax.biosolr.pdbe.fasta.FastaJob;

public class PhmmerClient {

  private static final Logger LOG = LoggerFactory.getLogger(PhmmerClient.class);
  
  private String phmmerUrl;
  
  public PhmmerClient(String phmmerUrl) {
      this.phmmerUrl = phmmerUrl;
  }

  public JsonObject getResults(String database, String sequence) throws IOException {
    String respUrl = getResultsUrl(database, sequence);
    LOG.debug("response URL=" + respUrl);
    return getResultsJson(respUrl);
  }
  
  private String getResultsUrl(String database, String sequence) throws IOException {
    LOG.info("getting PHMMER data for seqdb=" + database + "; sequence=" + sequence);
    try (HttpConnection http = new HttpConnection(phmmerUrl)) {
      http.post("seqdb=" + database + "&seq=>Seq%0D%0A" + sequence);
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
      LOG.debug("opening connection to " + url);
      http = (HttpURLConnection)new URL(url).openConnection();
    }
    
    private void post(String params) throws IOException {
      LOG.debug("POSTing \"" + params + "\"");
      http.setRequestMethod("POST");
      http.setDoOutput(true);
      http.setInstanceFollowRedirects(false);
      http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
      http.setRequestProperty("Content-Length", Integer.toString(params.getBytes().length));
      try (OutputStream out = http.getOutputStream()) {
        out.write(params.getBytes());
      }
      
      LOG.debug("response is " + http.getResponseCode() + " " + http.getResponseMessage());
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
