package uk.co.flax.biosolr.pdbe.fasta;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ebi.webservices.axis1.stubs.fasta.InputParameters;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_PortType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FastaJob implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(FastaJob.class);

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
  private Pattern pattern4 = Pattern.compile("^EMBOS[S ] (\\s*.*?)$");
  private Pattern pattern5 = Pattern.compile("^PDB:.*? (\\s*.*?)$|^PRE_PD.*? (\\s*.*?)$");

  public IOException getException() {
    return exception;
  }

  public String getStatus() {
    return status;
  }

  public FastaJobResults getResults() throws IOException {
    if (results == null) {
      byte[] result = getRawResults();
      InputStream in = new ByteArrayInputStream(result);
      results = parseResults(new BufferedReader(new InputStreamReader(in)));
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
      LOG.debug("FastaJob.run");
      jobId = fasta.run(email, "", params);
      LOG.debug("jobId=" + jobId);

      do {
        Thread.sleep(200);
        status = fasta.getStatus(jobId);
        LOG.debug("status=" + status);
      } while (status.equals(FastaStatus.RUNNING) || status.equals(FastaStatus.QUEUED));

      if (!status.equals(FastaStatus.DONE)) {
        LOG.error("Error with job: " + jobId + " (" + status + ")");
      }
    } catch (InterruptedException e) {
      interrupted = true;
    } catch (IOException e) {
      exception = e;
    }
  }

  // create an Alignment from a matching line
  private PDb.Alignment parseAlignment(Matcher matcher) {
    int n = firstGroup(matcher);
    String pdbIdChain = matcher.group(n);
    if (pdbIdChain.contains("Entity")) {
      pdbIdChain = pdbIdChain.replaceFirst(" ", "_");
    }
    String[] s = pdbIdChain.split("_");
    double eValue = new Double(matcher.group(n + 2));
    return new PDb.Alignment(new PDb.Id(s[0]), s[1], eValue);
  }

  private FastaJobResults parseResults(BufferedReader reader) throws IOException {
    results = new FastaJobResults();

    String line = "";
    while (line != null) {
      Matcher matcher1 = pattern1.matcher(line);
      Matcher matcher2 = pattern2.matcher(line);
      if (matcher1.find()) {
        PDb.Alignment alignment = parseAlignment(matcher1);
        results.addAlignment(alignment);
        line = reader.readLine();
      } else if (matcher2.find()) {
        int n = firstGroup(matcher2);
        String pdbIdChain = matcher2.group(n);
        if (pdbIdChain.contains("Entity")) {
          pdbIdChain = pdbIdChain.replaceFirst(" ", "_");
        }
        String[] bits = pdbIdChain.split("_");
        PDb.Alignment a = results.getAlignment(bits[0], bits[1]);
        if (a == null) {
          throw new RuntimeException("Alignment not yet seen: " + pdbIdChain);
        }

        // sometimes an alignment appears twice in the results - need to ignore all
        // but the first (but still need to consume lines)
        boolean complete = a.isComplete();

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
            if (! complete) {
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
            if (! complete) {
              a.addQuerySequence(m4.group(1));
            }
          } else if (m5.find()) {
            int n4 = firstGroup(m5);
            if (! complete) {
              a.addReturnSequence(m5.group(n4));
            }
          }
        }
      } else {
        line = reader.readLine();
      }
    }

    return results;
  }

}