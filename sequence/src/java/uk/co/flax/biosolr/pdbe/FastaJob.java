package uk.co.flax.biosolr.pdbe;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ebi.webservices.axis1.stubs.fasta.InputParameters;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_PortType;

public class FastaJob implements Runnable {

  private static final Logger LOG = Logger.getLogger(FastaJob.class.getName());

  private JDispatcherService_PortType fasta;

  private String email;

  private InputParameters params;

  private FastaJobResults results;

  // job id created in run()
  private String jobId;

  // exception caught during run(), if any, and the run status
  private IOException exception;
  private String status;
  private boolean interrupted;

  // regexp patterns
  private Pattern pattern1 = Pattern.compile("^PDB:(.*?_.*?)\\s+(.+?)\\s+([0-9.e-]+?)$|^PRE_PDB:(\\w{4} Entity)\\s+(.+?)\\s+([0-9.e-]+?)$");
  private Pattern pattern2 = Pattern.compile("^>>PDB:(.*?_.*?)\\s+.*?$|^>>PRE_PDB:(\\w{4} Entity).*?$");
  private Pattern pattern3 = Pattern.compile("^Smith-Waterman score:.*?\\;(.*?)\\% .*? overlap \\((.*?)\\)$");
  private Pattern pattern4 = Pattern.compile("^EMBOS  (\\s*.*?)$");
  private Pattern pattern5 = Pattern.compile("^PDB:.*? (\\s*.*?)$|^PRE_PD.*? (\\s*.*?)$");

  // sometimes an alignment appears twice in the results - need to ignore all
  // but
  // the first, so remember those we have completed
  private Set<Alignment> completeAlignments = new HashSet<>();

  public IOException getException() {
    return exception;
  }

  public String getStatus() {
    return status;
  }

  public FastaJobResults getResults() throws IOException {
    if (results == null) {
      String id = fasta.getResultTypes(jobId)[0].getIdentifier();
      byte[] result = fasta.getResult(jobId, id, null);
      InputStream in = new ByteArrayInputStream(result);
      results = parseResults(new BufferedReader(new InputStreamReader(in)));
      results.chooseShownAlignments();
    }
    return results;
  }

  public byte[] getRawResults() throws RemoteException {
    String id = fasta.getResultTypes(jobId)[0].getIdentifier();
    return fasta.getResult(jobId, id, null);
  }

  public boolean isInterrupted() {
    return interrupted;
  }

  public boolean resultsOk() {
    return exception == null && !interrupted && status.equals(FastaStatus.DONE);
  }

  public FastaJob(JDispatcherService_PortType fasta, String email, InputParameters params) {
    this.fasta = fasta;
    this.email = email;
    this.params = params;
    jobId = null;
    results = null;
    exception = null;
    status = null;
    interrupted = false;
  }

  public String getEmail() {
    return email;
  }

  public InputParameters getParams() {
    return params;
  }

  private int firstGroup(Matcher m) {
    for (int n = 1; n <= m.groupCount(); ++n) {
      if (m.group(n) != null) {
        return n;
      }
    }
    return 0;
  }

  public void run() {
    try {
      jobId = fasta.run(email, "", params);

      do {
        Thread.sleep(200);
        status = fasta.getStatus(jobId);
        LOG.log(Level.FINE, status);
      } while (status.equals(FastaStatus.RUNNING));

      if (!status.equals(FastaStatus.DONE)) {
        LOG.log(Level.SEVERE, "Error with job: " + jobId + " (" + status + ")");
      }
    } catch (InterruptedException e) {
      interrupted = true;
    } catch (IOException e) {
      exception = e;
    }
  }

  // create an Alignment from a matching line
  private Alignment parseAlignment(Matcher matcher) {
    int n = firstGroup(matcher);
    String pdbIdChain = matcher.group(n);
    if (pdbIdChain.contains("Entity")) {
      pdbIdChain = pdbIdChain.replaceFirst(" ", "_");
    }
    String[] s = pdbIdChain.split("_");
    String pdbId = s[0];
    String chain = s[1];
    double eValue = new Double(matcher.group(n + 2));
    return new Alignment(pdbId, chain, eValue);
  }

  private FastaJobResults parseResults(BufferedReader reader) throws IOException {
    results = new FastaJobResults();

    String line = "";
    while (line != null) {
      Matcher matcher1 = pattern1.matcher(line);
      Matcher matcher2 = pattern2.matcher(line);
      if (matcher1.find()) {
        Alignment alignment = parseAlignment(matcher1);
        results.addAlignment(alignment);
        line = reader.readLine();
      } else if (matcher2.find()) {
        int n = firstGroup(matcher2);
        String pdbIdChain = matcher2.group(n);

        if (pdbIdChain.contains("Entity")) {
          pdbIdChain = pdbIdChain.replaceFirst(" ", "_");
        }

        Alignment a = results.getAlignment(pdbIdChain);
        assert a != null;

        // ignore second set of sequence details for this alignment
        // (but still need to consume lines)
        if (completeAlignments.contains(a)) {
          a = null;
        }

        while ((line = reader.readLine()) != null) {
          Matcher m2 = pattern2.matcher(line);
          Matcher m3 = pattern3.matcher(line);
          Matcher m4 = pattern4.matcher(line);
          Matcher m5 = pattern5.matcher(line);

          if (m3.find()) {
            double identity = new Double(m3.group(1));
            String overLap = m3.group(2);
            String[] o = overLap.split(":");
            String[] oIn = o[0].split("-");
            String[] oOut = o[1].split("-");
            if (a != null) {
              a.setPercentIdentity(identity);
              try {
                a.setQueryOverlapStart(Integer.valueOf(oIn[0]));
                a.setQueryOverlapEnd(Integer.valueOf(oIn[1]));
                a.setDbOverlapStart(Integer.valueOf(oOut[0]));
                a.setDbOverlapEnd(Integer.valueOf(oOut[1]));
              } catch (NumberFormatException e) {
                throw new IOException("Error parsing line: " + line);
              }
            }
          } else if (m2.find()) {
            break;
          } else if (m4.find()) {
            if (a != null) {
              a.addQuerySequence(m4.group(1));
            }
          } else if (m5.find()) {
            int n4 = firstGroup(m5);
            if (a != null) {
              a.addReturnSequence(m5.group(n4));
            }
          }
        }

        if (a != null) {
          completeAlignments.add(a);
        }
      } else {
        line = reader.readLine();
      }
    }

    return results;
  }

}