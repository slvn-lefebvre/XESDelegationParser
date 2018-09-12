package org.isep.xes;

import org.deckfour.xes.model.*;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by slefebvr on 06/03/17.
 */
public class LeavesXVisitor extends XVisitor {

    public static String getDayOfEvent(LocalDateTime eventDate) {

        return eventDate.getYear() + "-" + eventDate.getDayOfYear();
    }



    final Map<String, Set<String>> presences= new HashMap<>();

    public LeavesXVisitor() {
        super();
    }

    @Override
    public void visitTracePre(XTrace trace, XLog log) {
        super.visitTracePre(trace, log);
    }

    @Override
    public void visitEventPre(XEvent event, XTrace trace) {
        super.visitEventPre(event, trace);
        XAttribute resource = event.getAttributes().get("org:resource");
        XAttribute time = event.getAttributes().get("time:timestamp");
        LocalDateTime eventDate = LocalDateTime.parse(time.toString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String dayOfEvent = getDayOfEvent(eventDate);
        Set<String> users = presences.getOrDefault(dayOfEvent, new HashSet<>());
        users.add(resource.toString());
        presences.put(dayOfEvent, users);
    }

    public Map<String, Set<String>> getPresences() {
        return presences;
    }

}
