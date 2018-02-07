package uk.co.flax.examples.xjoin;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonStructure;

public class HttpConnection implements AutoCloseable {
  private HttpURLConnection http;

  public HttpConnection(String url) throws IOException {
    http = (HttpURLConnection)new URL(url).openConnection();
  }

  public JsonStructure getJson() throws IOException {
    http.setRequestMethod("GET");
    http.setRequestProperty("Accept", "application/json");
    try (InputStream in = http.getInputStream();
         JsonReader reader = Json.createReader(in)) {
      return reader.read();
    }
  }

  @Override
  public void close() {
    http.disconnect();
  }
}
