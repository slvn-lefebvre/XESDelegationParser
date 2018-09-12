package org.isep.sociallog.similarity;

import java.util.Map;

/**
 * Created by slefebvr on 06/04/17.
 */
public class APriori extends SimilarityFun {
    public APriori(Map<String, Double> node) {
        super(node);
    }

    @Override
    public Double transformEntry(String s, Map<String, Double> stringDoubleMap) {
        return node.getOrDefault(s,0.0);
    }
}
