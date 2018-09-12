package org.isep.sociallog;

import com.google.common.collect.Maps;

import java.util.*;

/**
 * Created by slefebvr on 13/03/17.
 */
public class NetCrawler extends Crawler {

    public NetCrawler() {
        super("NetCrawler");
    }

    @Override
    public Solution crawl(Map<String, Double> probMap, Set<String> presences, Obstacle obstacle) {
       //String taskUser = obstacle.taskId+"-"+obstacle.user;

        String user = null;
        if(probMap!=null && probMap.size() > 0) {
            double rnd = Math.random(),
                   decision =0.0;


            Set<String> possible = probMap.keySet();
            // filter on presence.
            possible.retainAll(presences);
            // recompute probas
            Double tmp = 0.0;
            for(String usr: possible)
               tmp+= probMap.get(usr);

            final Double total = tmp;
            Map<String, Double> m = Maps.transformValues(probMap, i -> 1.0*i/total);
            //System.out.println(m);
            // Draw from distribution
            Iterator<String> it = possible.iterator();
            while(it.hasNext() && decision < rnd) {
                user = it.next();
                decision +=m.get(user);
            }

        } else {
            return null;
        }

        return new Solution(obstacle, obstacle.taskId,new HashSet<>(Arrays.asList(user)), "delegation");


    }
}
