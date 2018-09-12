package org.isep.sociallog;

import java.util.*;

/**
 * Created by slefebvr on 13/03/17.
 */
public class RandomCrawler extends Crawler {

    public RandomCrawler() {
        super("RandomCrawler");
    }

    @Override
    public Solution crawl(Map<String, Double> probMap, Set<String> presences, Obstacle obstacle) {
        String taskUser = obstacle.taskId+"-"+obstacle.user;

        String user=null;

        if(probMap!=null && probMap.size() > 0) {
            Set<String> possible = probMap.keySet();
            // filter on presence.
            possible.retainAll(presences);
            if(possible.size()>0) {
                double dist = 1.0 / possible.size(),
                        rnd = Math.random(),
                        decision = 0.0;
                Iterator<String> iterator = possible.iterator();

                while (iterator.hasNext() && decision < rnd && !presences.contains(user)) {
                    decision += dist;
                    user = iterator.next();
                }
            } else { user = obstacle.user; }

        } else {
            user = obstacle.user;
        }

        return new Solution(obstacle, obstacle.taskId,new HashSet<>(Arrays.asList(user)), "delegation");
    }


}
