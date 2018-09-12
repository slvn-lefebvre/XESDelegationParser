package org.isep.xes;

import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.model.*;
import org.deckfour.xes.model.impl.XAttributeImpl;
import org.isep.sociallog.Obstacle;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by slefebvr on 21/02/17.
 */
public class ExtractionXVisitor extends XVisitor{


    private Map<String, Map<String,Integer>>
            resourceAvailabilityMap = new HashMap<>();
    private Set<String> transitions = new HashSet<>();
    private Set<String> users       = new HashSet();
    private Map<String,Set<String>>
                dayToUsers          = new HashMap<>();
    private Map<String,String>
            startSuspension          = new HashMap<>();

    private String currentCase  = "";
    private String currentState = "";
    private String currentUser  = "";
    private String currentActivity = "";

    private PrintWriter suspensionsWriter, user1Writer, timeWriter;
    private int nbCreations=0, nbUser1=0;


    public ExtractionXVisitor() {
        super();
        try {
            suspensionsWriter   = new PrintWriter("./suspensions.csv");
            user1Writer         = new PrintWriter("./user1.csv");
            timeWriter          = new PrintWriter("./time.csv");

        } catch(Exception e) {
            System.out.println(e);
            System.exit(-1);

        }
    }

    @Override
    public void visitTracePre(XTrace trace, XLog log) {
        super.visitTracePre(trace, log);


        currentCase = trace.getAttributes().get("concept:name").toString();
        currentState ="UNKNOWN";
        currentUser = "UNKNOWN";
        currentActivity = "UNKNOWN";
    }

    @Override
    public void visitTracePost(XTrace trace, XLog log) {
        super.visitTracePost(trace, log);
    }

    @Override
    public void visitEventPre(XEvent event, XTrace trace) {
        super.visitEventPre(event, trace);

        XAttribute transition =  event.getAttributes().get("lifecycle:transition");
        XAttribute conceptName = event.getAttributes().get("concept:name");
        XAttribute resource = event.getAttributes().get("org:resource");

        XAttribute time = event.getAttributes().get("time:timestamp");

        LocalDateTime eventDate = LocalDateTime.parse(time.toString(),  DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String dayOfEvent = eventDate.getYear() +";" + eventDate.getDayOfYear();

        if((currentUser.equals("User_1") || currentUser.equals("UNKNOWN"))
                && currentState.equals("suspend") && !currentState.equals(transition.toString())) {

            user1Writer.println(eventDate + " ; "
                                + currentState  + ";"
                                + transition    + ";"
                                + currentCase   + ";"
                                + currentActivity + ";"
                                + conceptName   + ";"
                                + currentUser   + ";"
                                + resource);
        }
        if(conceptName.toString().equals("A_Create Application")) {
            nbCreations++;
            if(resource.toString().equals("User_1"))
                nbUser1++;
        }

        /**
         *  Suspensions logging
         */

        String stateId = currentCase+";"+conceptName.toString();
        if(transition.toString().equals("suspend")
                && !currentState.equals("suspend"))
            startSuspension.put(stateId,eventDate.toString()+";"+resource.toString());


        if(currentState.equals("suspend")
            && !transition.toString().equals("suspend")
            && startSuspension.containsKey(stateId))
            suspensionsWriter.println(stateId+";"+startSuspension.get(stateId)+";"+eventDate+";"+resource.toString()+";"+dayOfEvent);

        currentState = transition.toString();
        currentUser = resource.toString();
        currentActivity = conceptName.toString();

        Set dayUsers = dayToUsers.getOrDefault(dayOfEvent, new HashSet<>());
        dayUsers.add(currentUser);
        dayToUsers.put(dayOfEvent,dayUsers);
        timeWriter.println(time.toString() + ";" + currentUser);


        if(!transitions.contains(currentState))
            transitions.add(currentState);
        if(!users.contains(currentUser))
            users.add(currentUser);

        updateWorkload(currentUser +";"+dayOfEvent,transition.toString(),1);
    }

    public void printAvailabilityMap(PrintWriter pw) {
        pw.print("Resource;Year;Day;");
        transitions.forEach(t -> pw.print(t+";"));
        pw.println();
        resourceAvailabilityMap.forEach((k,v) -> {
            StringBuilder sb = new StringBuilder(k + ";");
            transitions.forEach(t -> {
                sb.append(v.getOrDefault(t, 0));
                sb.append(";");
            });
            pw.println(sb.toString());
        });
    }

    public void printLeaves(PrintWriter pw) {
        pw.print("Year;Day;User");
        pw.println();
        dayToUsers.forEach((k,v) -> v.forEach(u -> pw.println(k + ";" +u )));

    }


    private void updateWorkload(String resource, String transition, int qt) {

        Map<String, Integer> userMap = resourceAvailabilityMap.getOrDefault(resource,new HashMap<>());
        qt += userMap.getOrDefault(transition,0);
        userMap.put(transition,qt);
        resourceAvailabilityMap.put(resource,userMap);
    }

    public Map<String, Map<String,Integer>> getResourcesAvailability() {
        return resourceAvailabilityMap;
    }

    public void printUser() {
        System.out.println("Nb Creations " + nbCreations +  " Nb  user 1 "+ nbUser1);
    }
    public void closeWriters() {

        this.suspensionsWriter.close();
        this.user1Writer.close();
        this.timeWriter.close();
    }

}
