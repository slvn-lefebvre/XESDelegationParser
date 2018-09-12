package org.isep.sociallog.similarity;

import com.google.common.collect.Sets;
import org.isep.sociallog.ReplacementNetwork;

import java.util.Map;
import java.util.Set;

/**
 * Created by slefebvr on 22/03/17.
 */
public class WeightedCommon extends SimilarityFun {
    final ReplacementNetwork rn;
    public WeightedCommon(Map<String, Double> node, ReplacementNetwork replacementNetwork) {
        super(node);
        this.rn = replacementNetwork;
    }


    @Override
    public Double transformEntry(String s, Map<String, Double> otherNode) {
        Set<String> commonNeighbors = Sets.intersection(node.keySet(),otherNode.keySet());
        Double score = 0.0;
        for(String c: commonNeighbors) {
            Map<String, Double> common = rn.getNeighbours(c);
            score += node.getOrDefault(c,0.0) * common.getOrDefault(s,0.0);
        }
        return score;
    }
}
