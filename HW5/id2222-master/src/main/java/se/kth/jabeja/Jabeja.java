package se.kth.jabeja;

import org.apache.log4j.Logger;
import se.kth.jabeja.config.Config;
import se.kth.jabeja.config.NodeSelectionPolicy;
import se.kth.jabeja.io.FileIO;
import se.kth.jabeja.rand.RandNoGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Jabeja {
  final static Logger logger = Logger.getLogger(Jabeja.class);
  private final Config config;
  private final HashMap<Integer/*id*/, Node/*neighbors*/> entireGraph;
  private final List<Integer> nodeIds;
  private int numberOfSwaps;
  private int round;
  private double T;
  private boolean resultFileCreated = false;
  private double alpha;
  private double delta;
  private double minEdgeCut;
  private int rounds;
  private double restart;
  private double initial_T;


  //-------------------------------------------------------------------
  public Jabeja(HashMap<Integer, Node> graph, Config config) {
    this.entireGraph = graph;
    this.nodeIds = new ArrayList(entireGraph.keySet());
    this.round = 0;
    this.numberOfSwaps = 0;
    this.config = config;
    this.T = config.getTemperature();
    this.delta = 0.1;
    this.alpha = 1.1;
    this.minEdgeCut = Double.POSITIVE_INFINITY;
    this.rounds = 1000;
    this.restart = 40;
    this.initial_T = 1.00;

  }


  //-------------------------------------------------------------------
  public void startJabeja() throws IOException {
    for (round = 0; round < rounds; round++) {

      //Task 2 (restarts)

      if (round % restart == 0) {
        T = initial_T;
        System.out.println("Restarting\n\n\n\n");
      }


      for (int id : entireGraph.keySet()) {
        sampleAndSwap(id);

      }

      //one cycle for all nodes have completed.
      //reduce the temperature
      saCoolDown();
      report();
    }

    System.out.println("Minimum edge cut: " + minEdgeCut);
  }

  /**
   * Simulated annealing cooling function
   */
  private void saCoolDown(){

    //Task 1
    /*
    if (T > 1)
      T -= delta;
    if (T < 1)
      T = 1;
    */

    //Task 2 SA

    if (T > 1)
      T  = 1;
    T *= delta;
    if (T < 0.00001)
      T = 0.00001F;

  }

  /**
   * Sample and swap algorithm at node p
   * @param nodeId
   */
  private void sampleAndSwap(int nodeId) {
    Node partner = null;
    Node nodep = entireGraph.get(nodeId);

    if (config.getNodeSelectionPolicy() == NodeSelectionPolicy.HYBRID
            || config.getNodeSelectionPolicy() == NodeSelectionPolicy.LOCAL) {
      // swap with random neighbors
      partner=findPartner(nodeId,getNeighbors(nodep));
    }

    if (config.getNodeSelectionPolicy() == NodeSelectionPolicy.HYBRID
            || config.getNodeSelectionPolicy() == NodeSelectionPolicy.RANDOM) {
      // if local policy fails then randomly sample the entire graph
      if (partner == null) {
        partner = findPartner(nodeId, getSample(nodeId));
      }

    }

    // swap the colors
    if (partner != null) {
      int colorp = nodep.getColor();
      nodep.setColor(partner.getColor());
      partner.setColor(colorp);
      numberOfSwaps++;

    }
  }

  //Task 2 SA and Extra task
  private double acceptanceProbability(double newE, double oldE) {
    //Task 2
    //return Math.pow(Math.E, (newE - oldE) / T);

    //Extra task
    return  1 / (1 + Math.pow(Math.E, (oldE - newE) / T));
  }

  public Node findPartner(int nodeId, Integer[] nodes){

    Node nodep = entireGraph.get(nodeId);
    Node bestPartner = null;
    Node bestSAPartner = null;
    double highestBenefit = 0;
    double highestSABenefit = 0;

    for (int qid: nodes) {
      Node nodeq = entireGraph.get(qid);

      if (nodeq.getColor() == nodep.getColor()) continue; //skip if same color

      int dpp = getDegree(nodep, nodep.getColor());
      int dqq = getDegree(nodeq, nodeq.getColor());
      double oldE = Math.pow(dpp, alpha) + Math.pow(dqq, alpha);

      int dpq = getDegree(nodep, nodeq.getColor());
      int dqp = getDegree(nodeq, nodep.getColor());

      double newE = Math.pow(dpq, alpha) + Math.pow(dqp, alpha);

      //Task 1
      if ((newE * this.T > oldE) && (newE > highestBenefit)) {
        bestPartner = nodeq;
        highestBenefit = newE;
      }

      //Task 2 SA

      else if (acceptanceProbability(newE, oldE) > Math.random() && newE > highestSABenefit){
        bestSAPartner = nodeq;
        highestSABenefit = newE;
      }

    }
    return (bestPartner != null) ? bestPartner : bestSAPartner;
  }

  /**
   * The the degreee on the node based on color
   * @param node
   * @param colorId
   * @return how many neighbors of the node have color == colorId
   */
  private int getDegree(Node node, int colorId){
    int degree = 0;
    for(int neighborId : node.getNeighbours()){
      Node neighbor = entireGraph.get(neighborId);
      if(neighbor.getColor() == colorId){
        degree++;
      }
    }
    return degree;
  }

  /**
   * Returns a uniformly random sample of the graph
   * @param currentNodeId
   * @return Returns a uniformly random sample of the graph
   */
  private Integer[] getSample(int currentNodeId) {
    int count = config.getUniformRandomSampleSize();
    int rndId;
    int size = entireGraph.size();
    ArrayList<Integer> rndIds = new ArrayList<Integer>();

    while (true) {
      rndId = nodeIds.get(RandNoGenerator.nextInt(size));
      if (rndId != currentNodeId && !rndIds.contains(rndId)) {
        rndIds.add(rndId);
        count--;
      }

      if (count == 0)
        break;
    }

    Integer[] ids = new Integer[rndIds.size()];
    return rndIds.toArray(ids);
  }

  /**
   * Get random neighbors. The number of random neighbors is controlled using
   * -closeByNeighbors command line argument which can be obtained from the config
   * using {@link Config#getRandomNeighborSampleSize()}
   * @param node
   * @return
   */
  private Integer[] getNeighbors(Node node) {
    ArrayList<Integer> list = node.getNeighbours();
    int count = config.getRandomNeighborSampleSize();
    int rndId;
    int index;
    int size = list.size();
    ArrayList<Integer> rndIds = new ArrayList<Integer>();

    if (size <= count)
      rndIds.addAll(list);
    else {
      while (true) {
        index = RandNoGenerator.nextInt(size);
        rndId = list.get(index);
        if (!rndIds.contains(rndId)) {
          rndIds.add(rndId);
          count--;
        }

        if (count == 0)
          break;
      }
    }

    Integer[] arr = new Integer[rndIds.size()];
    return rndIds.toArray(arr);
  }


  /**
   * Generate a report which is stored in a file in the output dir.
   *
   * @throws IOException
   */
  private void report() throws IOException {
    int grayLinks = 0;
    int migrations = 0; // number of nodes that have changed the initial color
    int size = entireGraph.size();

    for (int i : entireGraph.keySet()) {
      Node node = entireGraph.get(i);
      int nodeColor = node.getColor();
      ArrayList<Integer> nodeNeighbours = node.getNeighbours();

      if (nodeColor != node.getInitColor()) {
        migrations++;
      }

      if (nodeNeighbours != null) {
        for (int n : nodeNeighbours) {
          Node p = entireGraph.get(n);
          int pColor = p.getColor();

          if (nodeColor != pColor)
            grayLinks++;
        }
      }
    }

    int edgeCut = grayLinks / 2;
    if (edgeCut < minEdgeCut)
      minEdgeCut = edgeCut;

    logger.info("round: " + round +
            ", edge cut:" + edgeCut +
            ", swaps: " + numberOfSwaps +
            ", migrations: " + migrations);

    saveToFile(edgeCut, migrations);
  }

  private void saveToFile(int edgeCuts, int migrations) throws IOException {
    String delimiter = "\t\t";
    String outputFilePath;

    //output file name
    File inputFile = new File(config.getGraphFilePath());
    outputFilePath = config.getOutputDir() +
            File.separator +
            inputFile.getName() + "_" +
            "NS" + "_" + config.getNodeSelectionPolicy() + "_" +
            "GICP" + "_" + config.getGraphInitialColorPolicy() + "_" +
            "T" + "_" + config.getTemperature() + "_" +
            "D" + "_" + delta + "_" +
            "RNSS" + "_" + config.getRandomNeighborSampleSize() + "_" +
            "URSS" + "_" + config.getUniformRandomSampleSize() + "_" +
            "A" + "_" + alpha + "_" +
            "R" + "_" + rounds + ".txt";

    if (!resultFileCreated) {
      File outputDir = new File(config.getOutputDir());
      if (!outputDir.exists()) {
        if (!outputDir.mkdir()) {
          throw new IOException("Unable to create the output directory");
        }
      }
      // create folder and result file with header
      String header = "# Migration is number of nodes that have changed color.";
      header += "\n\nRound" + delimiter + "Edge-Cut" + delimiter + "Swaps" + delimiter + "Migrations" + delimiter + "Skipped" + "\n";
      FileIO.write(header, outputFilePath);
      resultFileCreated = true;
    }

    FileIO.append(round + delimiter + (edgeCuts) + delimiter + numberOfSwaps + delimiter + migrations + "\n", outputFilePath);
  }
}
