package org.isep.sociallog.similarity;

import java.util.Map;
import java.util.Random;

/**
 * Created by slefebvr on 23/03/17.
 */
public class RandomSim extends SimilarityFun {
    Random ran = new Random();
    public RandomSim(Map<String, Double> node) {
        super(node);
    }

    @Override
    public Double transformEntry(String s, Map<String, Double> stringDoubleMap) {
        return ran.nextDouble();
    }
}
