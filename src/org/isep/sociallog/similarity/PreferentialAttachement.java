package org.isep.sociallog.similarity;

import java.util.Map;

/**
 * Created by slefebvr on 22/03/17.
 */
public class PreferentialAttachement extends SimilarityFun {
    public PreferentialAttachement(Map<String, Double> node) {
        super(node);
    }


    @Override
    public Double transformEntry(String s, Map<String, Double> otherUser) {
        return new Double(node.size() * otherUser.size()) ;
    }
}
