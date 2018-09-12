package org.isep.sociallog.similarity;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * Created by slefebvr on 20/03/17.
 */
public class CommonNeighbors extends SimilarityFun {
    public CommonNeighbors(Map<String, Double> node) {
        super(node);
    }

    @Override
    public Double transformEntry(String s, Map<String, Double> otherUser) {
        Set<String> inSet = this.node.keySet(),
                compSet = otherUser.keySet();

        return new Double(Sets.intersection(inSet, compSet).size());
    }
}
