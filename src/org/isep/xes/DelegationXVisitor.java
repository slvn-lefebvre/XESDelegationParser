package org.isep.xes;

import com.google.common.collect.Maps;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.XVisitor;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by slefebvr on 06/03/17.
 */
public class DelegationXVisitor extends XVisitor{

    final Map<String, Map<String, Integer>> taskUserDelegation = new HashMap<>();
    String currentUser, currentTask;

    public DelegationXVisitor() {
        super();

    }

    @Override
    public void visitTracePre(XTrace trace, XLog log) {
        super.visitTracePre(trace, log);
        currentUser = "UNKNOWN";
        currentTask = "UNKNOWN";
    }

    @Override
    public void visitEventPost(XEvent event, XTrace trace) {
        super.visitEventPost(event, trace);
        String task = event.getAttributes().get("concept:name").toString();
        String user = event.getAttributes().get("org:resource").toString();

        // delegation -> transition to another user in the same task
        if(task.equals(currentTask) && !user.equals(currentUser)) {

            String taskUser = currentUser;
            Map<String,Integer> users = taskUserDelegation.getOrDefault(taskUser, new HashMap<>());
            int count = users.getOrDefault(user,0) + 1;
            users.put(user,count);
            taskUserDelegation.put(taskUser,users);
        }
        currentTask = task;
        currentUser = user;
    }

    public Map<String, Map<String,Integer>> getDelegation() {
        return taskUserDelegation;
    }

    public static Map<String, Double>
        delegationProba(Map<String,Integer> data) {

          Integer total = data.values().stream().reduce((v1, v2) -> v1 + v2).orElse(1);
          Map<String, Double> m = Maps.transformValues(data, i -> 1.0 * i / total);
          return m;

    }


}
