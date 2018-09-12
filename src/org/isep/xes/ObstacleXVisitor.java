package org.isep.xes;

import org.deckfour.xes.model.*;
import org.isep.sociallog.*;
import org.isep.sociallog.similarity.JaccardSimilarity;
import org.isep.sociallog.similarity.SimilarityFactory;
import org.isep.sociallog.similarity.WeightedCommon;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by slefebvr on 06/03/17.
 */
public class ObstacleXVisitor extends XVisitor {

    public static final Set<String> EXCLUDED_TASKS = new HashSet<>();
    static {
        EXCLUDED_TASKS.add("W_Call after offers");
        EXCLUDED_TASKS.add("W_Call incomplete files");
    }

    public final static String NO_USER = "User_1";
    public final static String UNKNOWN = "UNKNOWN";

   // private final DelegationXVisitor delegationVisitor = new DelegationXVisitor();
    private final Map<String, Set<String>> presences;

    private final Set<String> actionSet = new HashSet<>(),
                                users = new HashSet<>(),
                                states = new HashSet<>();

    private final SolutionRepository solutionRepo = new SolutionRepository();
    private final Map<String, Map<String,Double>>  userWorkloads = new HashMap<>();

    private final Map<Obstacle,Map<String,Double>> tentativeSolutions = new HashMap<>();
    //private final  Map<String, Map<String,Integer>> perUTSolutionDistribution = new HashMap<>();
    private final Map<String, Long> clientSuspensions = new HashMap<>();
    private final Map<String, LocalDateTime> clientTaskStart = new HashMap<>(),
                                                taskStart =new HashMap<>();

    private String currentUser, currentTask, currentState, currentCase;

    private final LocalDateTime startSolutions;
    private LocalDateTime startTrace,currentDate;
    private boolean aborted;
    private int crawledEvents =0, nbObstacles=0,relevant, total;
    private final ReplacementNetwork replacements = new ReplacementNetwork();
    private final Crawler crawler;
    private final PrintWriter pw = new PrintWriter("./cases.csv"),
                                tdw = new PrintWriter("./tasksDurations.csv");


    private Map<Category, Integer> obstaclesNB = new HashMap<>();

    public ObstacleXVisitor(Map<String, Set<String>> presences, Crawler crawler,  LocalDateTime startSolutions) throws FileNotFoundException {
        super();
        this.presences = presences;
        this.startSolutions = startSolutions;
        this.crawler = crawler;
        states.add("takeover");
        states.add("lastsolution");
        tdw.println("caseId;taskId;startTime;endTime");
    }

    @Override
    public void visitTracePre(XTrace trace, XLog log) {
        super.visitTracePre(trace, log);
        currentCase = trace.getAttributes().get("concept:name").toString();

        clientSuspensions.put(currentCase,0L);
        currentUser = UNKNOWN;
        currentState = UNKNOWN;
        currentTask = UNKNOWN;
        nbObstacles = 0;
        startTrace = null;
        currentDate = startTrace;
        aborted = false;
        //delegationVisitor.visitTracePre(trace,log);
        tentativeSolutions.clear();

    }

    @Override
    public void visitTracePost(XTrace trace, XLog log) {
        super.visitTracePost(trace,log);
        long time = ChronoUnit.HOURS.between(startTrace,currentDate);
        pw.println(currentCase + ";" + time + ";"+ aborted + ";"+ nbObstacles+";"+clientSuspensions.getOrDefault(currentCase,0L));
    }

    /**
     * Applies the obstacle detection algorithm to detect an obstacle.
     * If an obstacle is found will look for a tentative solution in the repository of solutions and the crawler
     *
     * @param event
     * @param trace
     */
    @Override
    public void visitEventPre(XEvent event, XTrace trace) {
        super.visitEventPre(event, trace);
        // We assume User_1 is "no user"

        XAttributeMap xam = event.getAttributes();
        String eventId = xam.get("EventID").toString();
        String user = xam.get("org:resource").toString();
        String state = xam.get("lifecycle:transition").toString();
        users.add(user);

        if(!aborted && state.equals("ate_abort"))
            aborted = true;

        String task = xam.get("concept:name").toString();
        String action = xam.get("Action").toString();
        actionSet.add(action);
        states.add(state);

        XAttribute time = event.getAttributes().get("time:timestamp");
        LocalDateTime eventDate = LocalDateTime.parse(time.toString(),  DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        currentDate = eventDate;
        if(startTrace==null)
            startTrace = currentDate;

        updateWorkload(user,state,LeavesXVisitor.getDayOfEvent(eventDate));
        if(currentTask.equals(task) && !currentUser.equals(user))
            replacements.addReplacement(currentUser,user);

        if(EXCLUDED_TASKS.contains(task)) {
            if(state.equals("schedule"))
                clientTaskStart.put(currentCase, eventDate);
            else if(state.equals("complete") || state.equals("ate_abort")) {
                Long len = clientSuspensions.getOrDefault(currentCase,0L);
                len += ChronoUnit.HOURS.between(clientTaskStart.getOrDefault(currentCase,currentDate),currentDate);
                clientSuspensions.put(currentCase, len);
            }


        } else {
            if(state.equals("start"))  {
                taskStart.put(currentCase + ";" + currentTask, eventDate);
            } else if(state.equals("complete") || state.equals("ate_abort") || state.equals("pi_abort")) {
                tdw.println(currentCase+";"+currentTask
                        +";" + taskStart.getOrDefault(currentCase+";"+currentTask,LocalDateTime.now())
                        + ";" + eventDate);
            }
        }

        if(!EXCLUDED_TASKS.contains(task) && currentTask.equals(task)) {
            // 1. detect & register obstacles
            Obstacle obs = detectObstacle(state, task, user, eventDate, eventId, action);

            // 2. if obstacle detected try to solve & crawl.
            if (obs != null) {
                //Solution projected = findSolution(obs);
                nbObstacles++;
                tentativeSolutions.put(obs,
                        ReplacementNetwork.knnLinkSearch(
                                10,
                                replacements,
                                presences.getOrDefault(LeavesXVisitor.getDayOfEvent(eventDate), new HashSet<>()),
                                obs.getUser(),
                                SimilarityFactory.Similarity.JACCARD)
                        );
            }
        }


        /**
         * 3. when a solution appears
            unregister obstacle
            compare solutions
            Record solution
          */


        if(currentState.equals("suspend") && !state.equals("suspend") && !state.equals("ate_abort") && tentativeSolutions.size()>0) {
            Obstacle obs = tentativeSolutions.keySet().iterator().next();
            HashSet<String> resources = new HashSet<>();
            resources.add(user);
            Solution official = new Solution(obs, currentTask,resources, "delegation");
            solutionRepo.insertSolution(official);

            //System.out.println(replacements);


            //Map<String, Integer> dist = perUTSolutionDistribution.getOrDefault(obs.getUser()+  ";" + obs.getTask(),new HashMap<>() );
            //int num = dist.getOrDefault(official.getUser(),0);
            //dist.put(official.getUser(),num+1);
            //perUTSolutionDistribution.put(obs.getUser()+";"+obs.getTask(), dist);

            Map<String, Double> s = tentativeSolutions.remove(obs);

            if(eventDate.isAfter(startSolutions)) {
                //System.out.print(official);
                updateWorkload(official.getUser(),"takeover",LeavesXVisitor.getDayOfEvent(eventDate));

                total += s.size();
                relevant += s.keySet().contains(official.getUser()) ? 1 : 0;
            }
        }

        currentTask = task;
        currentUser = user;
        currentState = state;

    }

    public void updateWorkload(String user, String state, String day) {
        Map<String, Double> wk = userWorkloads.getOrDefault(user+"-"+day,new HashMap<>());
        Double qt = wk.getOrDefault(state,0.0);
        wk.put(state,qt+1);
        userWorkloads.put(user+"-"+day,wk);
    }

    /**
     * Tries to find a suitable solution for the specified obstacle
     * by using the local repository.
     * Uses the crawler if nothing is found in the repo.
     *
     * @param obs
     * @return
     */
    public Solution findSolution(Obstacle obs) {

        //Look for solution
        //1. look at the repo
        Solution sol =  solutionRepo.solveByObstacle(obs);
        if(sol==null) { //2. look at the crawler
           // Map<String, Double> model = DelegationXVisitor.delegationProba(delegationVisitor.getDelegation().getOrDefault(currentUser,new HashMap<>()));
            //sol = crawler.crawl(model, presences.get(LeavesXVisitor.getDayOfEvent(obs.getDateTime())), obs);
        }
        return sol;
    }

    public ReplacementNetwork getReplacementNetwork() {
        return replacements;
    }

    public Obstacle detectObstacle(String state, String task,String user, LocalDateTime eventDate, String eventId, String action) {
        if (currentState.equals("start")
                && state.equals("suspend")
                && user.equals(NO_USER)) {
            obstaclesNB.put(Category.LACK_EXECUTOR, obstaclesNB.getOrDefault(Category.LACK_EXECUTOR,0) +1);
            return new Obstacle(currentCase,task,eventDate,eventId, Category.LACK_EXECUTOR, action, currentUser);

        } else if (!presences.get(LeavesXVisitor.getDayOfEvent(eventDate)).contains(currentUser)
                && currentState.equals("suspend") && !state.equals("suspend")) {
            String taskUser = user;
           // Map<String, Integer> delegations = delegationVisitor.getDelegation().getOrDefault(taskUser, new HashMap<>());
            Category cat = replacements.getNetwork().containsKey(user) ? Category.EXPECTED_UNAVAILABILITY : Category.UNEXPECTED_UNAVAILABILITY;
            obstaclesNB.put(cat, obstaclesNB.getOrDefault(cat,0) +1);
            return new Obstacle(currentCase, currentTask, eventDate, eventId, cat, action, currentUser);

        }
        return null;
    }
    @Override
    public void visitEventPost(XEvent event, XTrace trace) {
        super.visitEventPost(event,trace);
        //delegationVisitor.visitEventPost(event,trace);

    }


    public void printObstaclesOut() {
        System.out.println(obstaclesNB);
    }


    public void printReport() {
            states.forEach(st -> System.out.print(st));
            System.out.println();
            //printSolutionsReport();
            //printReplacementGraph();
            //printCollaborationGraph();
            System.out.println(crawler.getName()
                                + " got "
                                + (1.0*crawler.getRights() / crawledEvents)*100
                                + " % right on " + crawledEvents);

            System.out.println("Precision : " + relevant + "/" + total + " = " + 1.0* relevant /total);

            try(PrintWriter pw = new PrintWriter("./solutionsWorkload.csv")) {
                pw.println("case;task;timestamp;obstacle;action;obstacle_user;solution;suspend;resume;schedule;takeover;start;complete;ate_abort;withdraw");
                solutionRepo.getSolutions().forEach(s -> {
                    pw.print(s);
                    Map<String, Double> wk  = userWorkloads.getOrDefault(s.getUser()+"-"+LeavesXVisitor.getDayOfEvent(s.getDate()),new HashMap<>());
                    states.forEach(st -> pw.print(";" + wk.getOrDefault(st,0.0)));
                    pw.println();
                });

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

    }

    public void printSolutionsReport()  {
        System.out.print("User;Task;");
        users.forEach(u -> System.out.print(u+";"));
        System.out.println();
       /* perUTSolutionDistribution.forEach((userTask, dist) -> {
            System.out.print(userTask + ";");
            users.forEach(u -> System.out.print(dist.getOrDefault(u,0) + ";"));
            System.out.println();

        });*/

    }

    public void closeWriters() {
        taskStart.forEach((k,v) -> System.out.println(k + ": " + v));
        pw.close();
        tdw.close();
    }


    public SolutionRepository getSolutionRepository() {
        return solutionRepo;
    }

    /**
     * digraph G {
        size ="4,4";
        main [shape=box];
        main -> parse [weight=8];


    public void printReplacementGraph() {
        HashMap<String, Integer> userPairs = new HashMap<>();
        perUTSolutionDistribution.forEach((userTask,dist) -> {
            String[] ut = userTask.split(";");
            String user1 = ut[0];
            dist.forEach((user2, i) -> {
                int weight = userPairs.getOrDefault(user1+ " -> " + user2,0);
                userPairs.put(user1+" -> "+ user2,weight+i);
            });
        });

        try (PrintWriter graphWriter = new PrintWriter("./graph.dot")) {
            graphWriter.println("digraph Replacement {");
            userPairs.forEach((p,w) -> graphWriter.println("\t" + p + " [weight="+w+"];"));
            graphWriter.println("}");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
    public void printCollaborationGraph() {
        try (PrintWriter graphWriter = new PrintWriter("./graph.dot")) {
            graphWriter.println("strict graph {");
            perUTSolutionDistribution.forEach((userTask, dist) -> dist.forEach((u, w) -> graphWriter.println(userTask + " -- " + u + " [weight=" + w + "]")));
            graphWriter.println("}");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }
     */

}
