package org.isep.sociallog;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Created by slefebvr on 07/03/17.
 */
public class Solution implements Comparable<Solution> {

    final Obstacle obstacle;
    final String task;
    final Set<String> resource;
    final String relation;


    public Solution(Obstacle o, String task, Set<String> resource, String rel) {
        this.obstacle = o;
        this.relation = rel;
        this.resource = resource;
        this.task = task;
    }


    @Override
    public int compareTo(Solution solution) {
        if(this.equals(solution))
            return 0;
        else if(solution.obstacle.compareTo(obstacle)==0
                    && solution.task.compareTo(task)==0
                    && solution.resource.containsAll(resource))
                return 0;
        else
            return solution.resource.containsAll(resource) ? 1 : -1;

    }
    public String toString() {
        return new String (obstacle.toString() +";"+ getUser());
    }
    public String getUser() {
        return resource.iterator().next();
    }

    public LocalDateTime getDate() {
        return obstacle.dateTime;
    }

    public Obstacle getObstacle() {
        return obstacle;
    }
}
