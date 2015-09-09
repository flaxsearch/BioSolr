package uk.co.flax.biosolr.pdbe.phmmer;

import java.io.IOException;

public class Main {

  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Arguments: [sequence]");
      return;
    }
    PhmmerJob job = new PhmmerJob("pdb");
    try {
      PhmmerResults results = job.getResults(args[0]);
    } catch (IOException e) {
      System.err.println("Error fetching PHMMER results");
      e.printStackTrace(System.err);
    }
  }

}
