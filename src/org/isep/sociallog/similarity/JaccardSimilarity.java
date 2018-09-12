package org.isep.sociallog.similarity;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * Created by slefebvr on 20/03/17.
 */
public class JaccardSimilarity extends SimilarityFun {
    public JaccardSimilarity(Map<String,Double> user) {
        super(user);
    }

    @Override
    public Double transformEntry(String s, Map<String, Double> otherUser) {
        Set<String> inSet = this.node.keySet(),
                compSet = otherUser.keySet();

        int inter = Sets.intersection(inSet, compSet).size(),
                union = Sets.union(inSet,compSet).size();


        return 1.0*inter / union;
    }
}
