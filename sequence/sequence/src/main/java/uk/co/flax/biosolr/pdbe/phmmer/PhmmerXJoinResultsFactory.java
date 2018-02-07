package uk.co.flax.biosolr.pdbe.phmmer;

//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//import static org.mockito.Matchers.any;

//import java.io.ByteArrayInputStream;
import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

//import javax.json.Json;
//import javax.json.JsonObject;
//import javax.json.JsonReader;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.xjoin.XJoinResults;
import org.apache.solr.search.xjoin.XJoinResultsFactory;

public class PhmmerXJoinResultsFactory implements XJoinResultsFactory<String> {
  
  private static final Logger LOG = Logger.getLogger(PhmmerXJoinResultsFactory.class.getName());

  // initialisation parameters
  public static final String INIT_DATABASE = "database";
  public static final String INIT_DEBUG_FILE = "debug.file";
  public static final String PHMMER_URL = "url";

  // request parameters
  public static final String PHMMER_SEQUENCE = "sequence";

  private PhmmerClient client;
  
  private String database;

  @Override
  @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
    String debugFile = (String) args.get(INIT_DEBUG_FILE);
    if (debugFile != null) {
//      try {
//        byte[] result = Files.readAllBytes(Paths.get(debugFile));
//        client = mock(PhmmerClient.class);
//        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(result))) {
//          JsonObject json = reader.readObject();
//          if (json == null) {
//            throw new RuntimeException("Can not initialise from " + INIT_DEBUG_FILE);
//          }
//          when(client.getResults(any(String.class), any(String.class))).thenReturn(json);
//        }
//      } catch (IOException e) {
//        throw new RuntimeException(e);
//      }
        throw new UnsupportedOperationException("debug file not supported");
    } 
    else {
      String url = (String) args.get(PHMMER_URL);    
      LOG.info("creating PHMMER client with URL " + url);
      client = new PhmmerClient(url);
    }

    database = (String)args.get(INIT_DATABASE);
    LOG.info("PHMMER database is " + database);
  }

  @Override
  public XJoinResults<String> getResults(SolrParams params) throws IOException {
    String sequence = params.get(PHMMER_SEQUENCE);
    if (sequence == null || sequence.length() == 0) {
      throw new RuntimeException("Missing or empty sequence");
    }
    PhmmerJob job = new PhmmerJob(client, database, sequence);
    return new Results(job.runJob());
  }
  
  public class Results implements XJoinResults<String> {

    private PhmmerResults results;
    
    private Results(PhmmerResults results) {
      this.results = results;
    }

    @Override
    public Iterable<String> getJoinIds() {
      Set<String> pdbIds = results.getPdbIds();
      String[] ids = pdbIds.toArray(new String[pdbIds.size()]);
      Arrays.sort(ids);
      return Arrays.asList(ids);
    }
    
    @Override
    public Collection<Alignment> getResult(String joinIdStr) {
      Map<String, Alignment> map = results.getAlignments().get(joinIdStr);
      return map != null ? map.values() : null;
    }

    public int getNumChains() {
      return results.getNumChains();
    }
    
    public int getNumEntries() {
      return results.getNumEntries();
    }
    
  }

}
