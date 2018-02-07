package uk.co.flax.biosolr.pdbe.phmmer;

import java.io.IOException;

public class Main {

  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Arguments: [url] [sequence]");
      return;
    }
    PhmmerClient client = new PhmmerClient(args[0]);
    PhmmerJob job = new PhmmerJob(client, "pdb", args[1]);
    try {
      PhmmerResults results = job.runJob();
      for (String pdbId : results.getPdbIds()) {
        System.out.println(pdbId);
      }
    } catch (IOException e) {
      System.err.println("Error fetching PHMMER results");
      e.printStackTrace(System.err);
    }
  }

}
