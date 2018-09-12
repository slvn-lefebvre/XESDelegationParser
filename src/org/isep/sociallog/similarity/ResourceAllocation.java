package org.isep.sociallog.similarity;

import com.google.common.collect.Sets;
import org.isep.sociallog.ReplacementNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by slefebvr on 23/03/17.
 */
public class ResourceAllocation extends SimilarityFun {

    private final Map<String, Map<String, Double>> net;

    public ResourceAllocation(Map<String, Double> node, ReplacementNetwork rn) {
        super(node);
        this.net = rn.getNetwork();
    }

    @Override
    public Double transformEntry(String s, Map<String, Double> otherUser) {
        Set<String> inSet = this.node.keySet(),
                compSet = otherUser.keySet();

        Set<String> common = Sets.intersection(inSet, compSet);
        double score =0.0;
        for(String x : common) {
            int z = net.getOrDefault(x, new HashMap<>()).size();
            if(z>0.0)
                score += 1/z;
        }
        return score;
    }
}
