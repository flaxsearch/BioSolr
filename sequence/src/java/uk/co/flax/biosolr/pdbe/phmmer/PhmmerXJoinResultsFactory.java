package uk.co.flax.biosolr.pdbe.phmmer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.xjoin.XJoinResults;
import org.apache.solr.search.xjoin.XJoinResultsFactory;

public class PhmmerXJoinResultsFactory implements XJoinResultsFactory<String> {
  
  // initialisation parameters
  public static final String INIT_DATABASE = "database";

  // request parameters
  public static final String PHMMER_SEQUENCE = "sequence";

  private String database;

  @Override
  @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
    database = (String)args.get(INIT_DATABASE);
  }

  @Override
  public XJoinResults<String> getResults(SolrParams params) throws IOException {
    String sequence = params.get(PHMMER_SEQUENCE);
    if (sequence == null || sequence.length() == 0) {
      throw new RuntimeException("Missing or empty sequence");
    }
    PhmmerClient client = new PhmmerClient();
    PhmmerJob job = new PhmmerJob(client, database, sequence);
    return new Results(job.runJob());
  }
  
  public class Results implements XJoinResults<String> {

    private PhmmerResults results;
    
    private Results(PhmmerResults results) {
      this.results = results;
    }
    
    @Override
    public Alignment getResult(String joinIdStr) {
      int chain = Integer.parseInt(joinIdStr.substring(joinIdStr.length() - 1)) + 'A' - 1;
      String pdbIdChain = joinIdStr.substring(0,  joinIdStr.length() - 1) + Character.valueOf((char)chain);
      return results.getAlignment(pdbIdChain);
    }

    @Override
    public Iterable<String> getJoinIds() {
      List<String> ids = new ArrayList<>();
      for (String id : results.getPdbIdChains()) {
        int chainId = (int)id.charAt(id.length() - 1) - (int)'A' + 1;
        ids.add(id.substring(0, id.length() - 1) + chainId);
      }
      return ids;
    }
    
    public int getNumEntries() {
      return results.getSize();
    }
    
  }

}
