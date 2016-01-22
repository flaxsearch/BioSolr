package uk.co.flax.examples.xjoin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.xjoin.XJoinResults;
import org.apache.solr.search.xjoin.XJoinResultsFactory;

public class ClickXJoinResultsFactory
implements XJoinResultsFactory<String> {

  private String url;
  
  @Override
  @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
    url = (String)args.get("url");
  }

  /**
   * Use 'click' REST API to fetch current click data. 
   */
  @Override
  public XJoinResults<String> getResults(SolrParams params)
  throws IOException {
    String apiUrl = url + "?q=" + params.get("q");
    try (HttpConnection http = new HttpConnection(apiUrl)) {
      JsonArray products = (JsonArray)http.getJson();
      return new ClickResults(products);
    }
  }
    
  public class ClickResults implements XJoinResults<String> {

    private Map<String, Click> clickMap;
    
    public ClickResults(JsonArray products) {
      clickMap = new HashMap<>();
      for (JsonValue product : products) {
        JsonObject object = (JsonObject)product;
        String id = object.getString("id");
        double weight = object.getJsonNumber("weight").doubleValue();
        clickMap.put(id, new Click(id, weight));
      }
    }
    
    public int getCount() {
      return clickMap.size();
    }
    
    @Override
    public Iterable<String> getJoinIds() {
      return clickMap.keySet();
    }

    @Override
    public Object getResult(String id) {
      return clickMap.get(id);
    }
      
  }
  
  public class Click {
    
    private String id;
    private double weight;
    
    public Click(String id, double weight) {
      this.id = id;
      this.weight = weight;
    }
    
    public String getId() {
      return id;
    }
    
    public double getWeight() {
      return weight;
    }
    
  }

}
