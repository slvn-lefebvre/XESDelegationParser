package org.isep.sociallog;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.isep.sociallog.similarity.PreferentialAttachement;
import org.isep.sociallog.similarity.SimilarityFactory;
import org.isep.sociallog.similarity.SimilarityFun;
import org.isep.sociallog.similarity.WeightedCommon;
import org.isep.xes.LeavesXVisitor;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Created by slefebvr on 20/03/17.
 */
public class ReplacementNetwork {

    public final static double INCREASE_WEIGHT = 4.0,
                        DECREASE_WEIGHT = 1.0;
    private final Map<String,Map<String, Double>> network =new HashMap<>();

    static private final Comparator<Map.Entry<String,Double>> ENTRY_COMPARATOR = Comparator.comparing(Map.Entry::getValue);

    public void addReplacement(String obstacleName,String replacement) {
        Map<String, Double> replacementMap = network.getOrDefault(obstacleName,new HashMap<>());
        //System.out.println(replacement + " " + absent);
        //System.out.println(replacementMap);

        double weight = replacementMap.getOrDefault(replacement,0.0);
        replacementMap.put(replacement,weight+INCREASE_WEIGHT);
        network.put(obstacleName,replacementMap);
    }

    public void decreaseWeight(String absent, String replacement) {
        Map<String, Double> replacementMap = network.getOrDefault(absent,new HashMap<>());
        double weight = replacementMap.getOrDefault(replacement,0.0);
        if(weight > 0.0) {
            replacementMap.put(replacement,weight-DECREASE_WEIGHT);
            network.put(absent,replacementMap);
        }
    }

    public void decreaseAllWeights() {
        network.keySet().forEach(k -> {
            Map<String, Double> weights = network.get(k);
            weights.forEach((r,w) -> {
                weights.put(r, w > 0.0 ? w-DECREASE_WEIGHT:0.0);
            });
            network.put(k,weights);
        });
    }

    public Map<String,Map<String,Double>> getNetwork() {
        return network;
    }

    public Map<String, Double> getNeighbours(String node) {
        return network.getOrDefault(node,new HashMap<>());
    }


    /**
     * Based on "Link Prediction Analysis in the Wikipedia
     Collaboration Graph" by F. Molnar
     * @param solutions
     * @param presences
     *
     */
    public static ReplacementNetwork evaluate(SimilarityFactory.Similarity similarity, int K, List<Solution> solutions, Map<String,Set<String>> presences) {
        PrintWriter pw=null;
        try {
            pw = new PrintWriter("solutions3.csv");
            pw.println("caseId;taskId;dateTime;category;action;obstacleUser;solutionUser;predictedUser;predictedPeering;actualPeering;nz");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String currentDay ="";
        int currentTime = 0, startEval = 5;
        int truePositives = 0, total=0, falsePositives= 0, falseNegatives=0;
        ReplacementNetwork rn = new ReplacementNetwork();


        for(Solution sol : solutions) {
            String day = LeavesXVisitor.getDayOfEvent(sol.getDate());
            //System.out.println(sol.getDate());
            String key = sol.getObstacle().getUser() + "-" + sol.getObstacle().getTask() + "-" + currentDay;

            if(!currentDay.equals(day)) {
                  currentTime++;
                // Update network weights
                rn.decreaseAllWeights();
                currentDay = day;
                if(currentTime > startEval)
                    total += presences.getOrDefault(day, new HashSet<>()).size();
            }

            if(currentTime > startEval) {

                //1. GET POTENTIAL SOLUTIONS FOR OBSTACLE
                Map<String, Double> results = knnLinkSearch(K,
                        rn,
                        presences.getOrDefault(day, new HashSet<>()),
                        sol.getObstacle().getUser(),
                        similarity);

                boolean found = results.containsKey(sol.getUser());
                truePositives +=  found ? 1:0;
                falsePositives += found ? results.size()-1: results.size();
                falseNegatives += found ? 0:1;
                Map.Entry<String,Double> first = results.entrySet().iterator().next();
                pw.println(sol.toString() + "; "+
                                        first.getKey() + ";"+ first.getValue() +";" +
                                        rn.getNeighbours(sol.getObstacle().getUser()).getOrDefault(sol.getUser(),0.0)
                                            + ";" + rn.getNeighbours(sol.getUser()).size() );

                printCommonNetworks(sol.getObstacle().caseId + " " + sol.getDate(),
                                      rn,
                            sol.obstacle.getUser(),
                            sol.getUser(),
                            first.getKey());

            }
            //2. UPDATE NETWORK
            rn.addReplacement(sol.getObstacle().user,sol.getUser());

        }
        pw.close();
        System.out.println(similarity+ ";"+K+";" + truePositives + ";" + falsePositives + ";" + falseNegatives +";"+ (total*K));
        return rn;
    }

    public static void printCommonNetworks(String id, ReplacementNetwork rn, String obstacle, String solution, String predicted) {
            try(PrintWriter gw = new PrintWriter("graphs/"+id+".dot")) {
            gw.println("digraph G {");
                gw.println(obstacle +" [label=\""+obstacle + "\",color=\"red\", fillcolor=\"red\"]");
                rn.getNeighbours(obstacle).forEach((u, w) -> {
                    if(w > 0.0) gw.println(obstacle + " -- " + u + " [weight=" + w + "]");
                });
                gw.println(solution +" [label=\""+solution + "\",color=\"green\", fillcolor=\"green\"]");
                rn.getNeighbours(solution).forEach((u,w) -> {
                    if(w > 0.0) gw.println(solution + " -- " + u + " [weight=" + w + "]");
                });
                gw.println(predicted +" [label=\""+predicted + "\",color=\"orange\", fillcolor=\"orange\"]");
                rn.getNeighbours(predicted).forEach((u,w) -> {
                    if(w > 0.0) gw.println(predicted + " -- " + u+ " [weight=" + w + "]");
                });
                gw.println("}");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
    }

    /**
     * Looks for K similar links based on the the specified similarity measure,
     * and the substitution network at hand
     * @param k
     * @param replacementNetwork
     * @param presents
     * @param user
     * @param similarity
     * @return
     */
    public static Map<String, Double> knnLinkSearch(int k, ReplacementNetwork replacementNetwork, Set<String> presents, String user, SimilarityFactory.Similarity similarity) {

        SimilarityFun  similarityFun = SimilarityFactory.getSim(similarity,user,replacementNetwork);

        Map<String,Double> similarityMap = Maps.transformEntries(replacementNetwork.getNetwork(), similarityFun);
        //System.out.println(similarityMap);

        Map<String, Double> updatedMap = new HashMap<>();
        presents.forEach(s -> updatedMap.put(s, similarityMap.getOrDefault(s,0.0)));
        updatedMap.remove(user);
        //System.out.println(updatedMap);
        Map<String,Double> out = new HashMap<>();

        if(updatedMap.size()>0)
             updatedMap.entrySet().stream()
                        .sorted(ENTRY_COMPARATOR.reversed())
                        .limit(k)
                        .collect(Collectors.toList())
                        .forEach(e -> out.put(e.getKey(),e.getValue()));
        //System.out.println(out);
        return out;
    }

    public static Map<String, Double> knnLinkSearch2(int k, ReplacementNetwork replacementNetwork, Set<String> presents, SimilarityFun similarity ) {

        Map<String,Double> similarityMap = Maps.transformEntries(replacementNetwork.getNetwork(), similarity);
        //System.out.println(similarityMap);

        Map<String, Double> updatedMap = new HashMap<>();
        presents.forEach(s -> updatedMap.put(s, similarityMap.getOrDefault(s,0.0)));
        //System.out.println(updatedMap);
        Map<String,Double> out = new HashMap<>();

        if(updatedMap.size()>0)
            updatedMap.entrySet().stream()
                    .sorted(ENTRY_COMPARATOR.reversed())
                    .limit(k)
                    .collect(Collectors.toList())
                    .forEach(e -> out.put(e.getKey(),e.getValue()));
        //System.out.println(out);
        return out;
    }
}
