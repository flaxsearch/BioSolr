package uk.co.flax.examples.xjoin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.xjoin.XJoinResults;
import org.apache.solr.search.xjoin.XJoinResultsFactory;

public class OfferXJoinResultsFactory implements XJoinResultsFactory<String> {

  private String url;
  
  private String idField;
  
  private String discountField;
  
	@Override
	@SuppressWarnings("rawtypes")
	public void init(NamedList args) {
	  url = (String)args.get("url");
	  idField = (String)args.get("idField");
	  discountField = (String)args.get("discountField");
	}

	/**
	 * Use 'offers' REST API to fetch current offer data. 
	 */
	@Override
	public XJoinResults<String> getResults(SolrParams params) throws IOException {
	  try (HttpConnection http = new HttpConnection(url)) {
	    JsonArray offers = (JsonArray)http.getJson();
	    return new OfferResults(offers);
	  }
	}
	  
	public class OfferResults implements XJoinResults<String> {

	  private JsonArray offers;
	  
	  public OfferResults(JsonArray offers) {
	    this.offers = offers;
	  }
	  
    public int getCount() {
      return offers.size();
    }
    
    @Override
    public Iterable<String> getJoinIds() {
      List<String> ids = new ArrayList<>();
      for (JsonValue offer : offers) {
        ids.add(((JsonObject)offer).getString(idField));
      }
      return ids;
    }

    @Override
    public Object getResult(String joinIdStr) {
      for (JsonValue offer : offers) {
        String id = ((JsonObject)offer).getString(idField);
        if (id.equals(joinIdStr)) {
          return new Offer(offer);
        }
      }
      return null;
    }
      
  }
	
	public class Offer {
	  
	  private JsonValue offer;
	  
	  public Offer(JsonValue offer) {
	    this.offer = offer;
	  }
    
    public double getDiscount() {
      return ((JsonObject)offer).getInt(discountField) * 0.01d;
    }
    
	}

}
