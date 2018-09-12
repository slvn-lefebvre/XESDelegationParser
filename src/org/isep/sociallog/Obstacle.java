package org.isep.sociallog;


import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Created by slefebvr on 23/02/17.
 */
public class Obstacle implements Comparable<Obstacle> {


    final String caseId;
    final String taskId;
    final String eventId;
    final Category category;
    final LocalDateTime dateTime;
    final String action;
    final String user;

    public Obstacle(String caseId,String taskId,LocalDateTime dateTime,  String eventId,  Category cat, String action, String user) {
        this.caseId = caseId;
        this.taskId = taskId;
        this.dateTime = dateTime;
        this.eventId = eventId;
        this.category = cat;
        this.action = action;
        this.user = user;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }
    public String getUser() {
        return user;
    }
    public String getTask() {
        return taskId;
    }

    public String toString() {
        return caseId +";" + taskId+ ";"+ dateTime+";"+category +";"+action+";"+user;
    }

    @Override
    public int compareTo(Obstacle obstacle) {

        if(this.equals(obstacle)) return 0;

        int comp = category.compareTo(obstacle.category);
        if(comp==0) comp = taskId.compareTo(obstacle.taskId);
        if(comp==0) comp = user.compareTo(obstacle.user);
        if(comp==0) comp = eventId.compareTo(obstacle.eventId);

        return comp;
    }

    public Category getCategory() {
        return category;
    }
}
