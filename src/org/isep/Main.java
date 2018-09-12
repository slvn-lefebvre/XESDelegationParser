package org.isep;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.out.XesXmlSerializer;
import org.isep.sociallog.*;
import org.isep.sociallog.similarity.SimilarityFactory;
import org.isep.xes.ExtractionXVisitor;
import org.isep.xes.LeavesXVisitor;
import org.isep.xes.ObstacleXVisitor;

import java.io.*;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        System.out.println("Trying to parse: " + args[0]);
        File f = new File(args[0]);
        System.out.println(f);
        XesXmlParser xp = new XesXmlParser();
        long start = System.currentTimeMillis();
        if(!xp.canParse(f)) {
            System.out.println("Can't parse " + args[0]);
            System.exit(0);
        }

        ReplacementNetwork rn = null;
        SolutionRepository sr = null;
        Map<String, Set<String>> presences = null;
        try (FileInputStream fis = new FileInputStream(f)) {

            List<XLog> logs = xp.parse(fis);

            System.out.println(logs.size());
            XLog xl = logs.get(0);
            System.out.println(xl.toString());
            for(XEventClassifier xc: xl.getClassifiers())
                System.out.println(xc.name() +  " " + xc.toString());
            /*
            ExtractionXVisitor oxv = new ExtractionXVisitor();
            xl.accept(oxv);
            PrintWriter leavesWriter = new PrintWriter("./leaves.csv"),
                        workloadWriter = new PrintWriter("./workload.csv");
            oxv.printAvailabilityMap(workloadWriter);
            oxv.printLeaves(leavesWriter);
            leavesWriter.close();
            workloadWriter.close();
            oxv.closeWriters();
            oxv.printUser();
            // */
            List<Crawler> crawlerList = new ArrayList<>();
            crawlerList.add(new NetCrawler());
            //crawlerList.add(new RandomCrawler());
            LeavesXVisitor lxv = new LeavesXVisitor();
            xl.accept(lxv);
            presences = lxv.getPresences();
            NetCrawler nc = new NetCrawler();
            ObstacleXVisitor oxv = new ObstacleXVisitor(lxv.getPresences(), nc, LocalDateTime.of(2016, Month.JANUARY,1,0,0));
            xl.accept(oxv);

            /*
            List<Obstacle> obstacles = oxv.getObstacleList();


            PrintWriter obstaclesWriter = new PrintWriter("./obstacles.csv");
            obstacles.forEach(x -> obstaclesWriter.println(x));

            obstaclesWriter.close();
            */
            //oxv.printReport();
            sr = oxv.getSolutionRepository();
            oxv.printObstaclesOut();
            oxv.closeWriters();
            rn = oxv.getReplacementNetwork();

            XesXmlSerializer xxs = new XesXmlSerializer();

            xxs.serialize(xl, new FileOutputStream("out.xml"));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<Solution> solutions = sr.getSolutions();
        Collections.sort(solutions, (solution, t1) -> solution.getObstacle().getDateTime().compareTo(t1.getDate()));
        for(SimilarityFactory.Similarity similarity: SimilarityFactory.Similarity.values()) {
            for (int k = 1; k < 2; k += 1) {
                ReplacementNetwork.evaluate(similarity, k, solutions, presences);
                //rn.getNetwork().forEach((name,net) -> System.out.println(name +" : " + net));

            }

        }
        //ReplacementNetwork.evaluate(SimilarityFactory.Similarity.APRIORI, 1, solutions, presences);
        System.out.println("End in : " + (System.currentTimeMillis() - start));

    }
}
