package uk.co.flax.biosolr.pdbe.phmmer;

import java.io.IOException;

public class Main {

  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Arguments: [sequence]");
      return;
    }
    PhmmerClient client = new PhmmerClient();
    PhmmerJob job = new PhmmerJob(client, "pdb", args[0]);
    try {
      PhmmerResults results = job.runJob();
      for (String pdbIdChain : results.getPdbIdChains()) {
        System.out.println(pdbIdChain);
      }
    } catch (IOException e) {
      System.err.println("Error fetching PHMMER results");
      e.printStackTrace(System.err);
    }
  }

}
