package org.isep.sociallog;

import java.util.Map;
import java.util.Set;

/**
 * Created by slefebvr on 13/03/17.
 */
public abstract class Crawler {
    int rights = 0;
    final String name;


    public Crawler(String name) {
        this.name = name;
    }

    public abstract Solution crawl(Map<String, Double> probMap, Set<String> presences, Obstacle obstacle);
    public void increaseRight() {
        rights ++;
    }
    public int getRights() {
        return rights;
    }

    public String getName() {
        return name;
    }
}
