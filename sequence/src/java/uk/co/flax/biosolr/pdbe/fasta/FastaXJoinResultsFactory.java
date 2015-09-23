package uk.co.flax.biosolr.pdbe.fasta;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import javax.xml.rpc.ServiceException;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.xjoin.XJoinResults;
import org.apache.solr.search.xjoin.XJoinResultsFactory;

import uk.ac.ebi.webservices.axis1.stubs.fasta.InputParameters;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_PortType;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_Service;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_ServiceLocator;
import uk.ac.ebi.webservices.axis1.stubs.fasta.WsResultType;

/**
 * Connect to FASTA service and generate a PDB id filter based on a user
 * supplied sequence.
 */
public class FastaXJoinResultsFactory implements XJoinResultsFactory<String> {

  // initialisation parameters
  public static final String INIT_EMAIL = "email";
  public static final String INIT_PROGRAM = "program";
  public static final String INIT_DATABASE = "database";
  public static final String INIT_STYPE = "stype";
  public static final String INIT_DEBUG_FILE = "debug.file";

  // request parameters
  public static final String FASTA_EXPLOWLIM = "explowlim";
  public static final String FASTA_EXPUPPERLIM = "expupperlim";
  public static final String FASTA_SEQUENCE = "sequence";
  public static final String FASTA_SCORES = "scores";
  public static final String FASTA_ALIGNMENTS = "alignments";
  public static final String FASTA_UNIQUE_PDB_IDS = "unique_pdb_ids";

  private JDispatcherService_PortType fasta;
  private String email;
  private String program;
  private String database;
  private String sType;

  @Override
  @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
    String debugFile = (String) args.get(INIT_DEBUG_FILE);
    if (debugFile != null) {
      try {
        byte[] result = Files.readAllBytes(Paths.get(debugFile));
        fasta = mock(JDispatcherService_PortType.class);
        when(fasta.getStatus(null)).thenReturn(FastaStatus.DONE);
        WsResultType[] types = new WsResultType[] { mock(WsResultType.class) };
        when(fasta.getResultTypes(null)).thenReturn(types);
        when(fasta.getResult(null, null, null)).thenReturn(result);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      JDispatcherService_Service service = new JDispatcherService_ServiceLocator();
      try {
        fasta = service.getJDispatcherServiceHttpPort();
      } catch (ServiceException e) {
        throw new RuntimeException(e);
      }
    }

    email = (String) args.get(INIT_EMAIL);
    program = (String) args.get(INIT_PROGRAM);
    database = (String) args.get(INIT_DATABASE);
    sType = (String) args.get(INIT_STYPE);
  }

  private String getParam(SolrParams params, String name) {
    String value = params.get(name);
    if (value == null || value.length() == 0) {
      throw new RuntimeException("Missing or empty " + name);
    }
    return value;
  }

  /**
   * Call out to the FASTA service and add a filter query based on the response.
   */
  @Override
  public XJoinResults<String> getResults(SolrParams params) throws IOException {
    InputParameters input = new InputParameters();
    input.setProgram(program);
    input.setDatabase(new String[] { database });
    input.setStype(sType);
    input.setSequence(getParam(params, FASTA_SEQUENCE));
    input.setExplowlim(new Double(getParam(params, FASTA_EXPLOWLIM)));
    input.setExpupperlim(new Double(getParam(params, FASTA_EXPUPPERLIM)));
    input.setScores(new Integer(getParam(params, FASTA_SCORES)));
    input.setAlignments(new Integer(getParam(params, FASTA_ALIGNMENTS)));

    FastaJob job = new FastaJob(fasta, email, input);
    job.run();

    if (!job.resultsOk()) {
      if (job.getException() != null) {
        throw new RuntimeException(job.getException());
      }
      if (!FastaStatus.DONE.equals(job.getStatus())) {
        throw new RuntimeException("Unexpected FASTA job status: "
            + job.getStatus());
      }
      if (job.isInterrupted()) {
        throw new RuntimeException("FASTA job was interrupted");
      }
      throw new RuntimeException("No results");
    }

    boolean unique = new Boolean(params.get(FASTA_UNIQUE_PDB_IDS));
    return new Results(job.getResults(), unique);
  }

  public static class Results implements XJoinResults<String> {

    private FastaJobResults results;
    private Map<String, Alignment> alignments;

    public Results(FastaJobResults results, boolean unique) {
      this.results = results;
      alignments = results.getAlignments(unique);
    }

    @Override
    public Iterable<String> getJoinIds() {
      String[] entries = new String[alignments.size()];
      int i = 0;
      for (Alignment a : alignments.values()) {
        entries[i++] = getEntryEntity(a);
      }
      Arrays.sort(entries);
      return Arrays.asList(entries);
    }

    @Override
    public Alignment getResult(String joinId) {
      String[] bits = joinId.split("_");
      if (bits.length != 2) {
        throw new RuntimeException("Bad entry_entity format: " + joinId);
      }
      String pdbId = bits[0].toUpperCase();
      int chain = Integer.parseInt(bits[1]) + 'A' - 1;
      String pdbIdChain = pdbId + "_" + Character.valueOf((char) chain);
      return alignments.get(pdbIdChain);
    }

    public int getNumChains() {
      return results.getNumChains();
    }

    public int getNumEntries() {
      return results.getNumEntries();
    }

  }

  public static String getEntryEntity(Alignment a) {
    // pdb SOLR entry_entity: lower case, and numbers for chain instead of
    // letters
    if (a.getChain().length() == 1) {
      int chainId = (int) a.getChain().charAt(0) - (int) 'A' + 1;
      return a.getPdbId().toLowerCase() + "_" + chainId;
    } else {
      // chain is "Entity" (from PRE_PDB entries)
      return a.getPdbId().toLowerCase() + "_" + a.getChain().toLowerCase();
    }
  }

}
