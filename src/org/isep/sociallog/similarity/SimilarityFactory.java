package org.isep.sociallog.similarity;

import org.isep.sociallog.ReplacementNetwork;

/**
 * Created by slefebvr on 23/03/17.
 */
public class SimilarityFactory {
    public enum Similarity {
        ADAMAR,
        COMMON_NB,
        JACCARD,
        PREFERENTIAL,
        WEIGHTED_COMMON,
        RESOURCE_ALLOCATION,
        RANDOM_SIM,
        APRIORI
    }
    public static SimilarityFun getSim(Similarity similarity, String node, ReplacementNetwork replacementNetwork) {
        SimilarityFun sf = null;
        switch(similarity) {
            case ADAMAR:
                sf = new AdamarSim(replacementNetwork.getNeighbours(node),replacementNetwork);
                break;
            case COMMON_NB:
                sf = new CommonNeighbors(replacementNetwork.getNeighbours(node));
                break;
            case PREFERENTIAL:
                sf = new PreferentialAttachement(replacementNetwork.getNeighbours(node));
                break;
            case WEIGHTED_COMMON:
                sf = new WeightedCommon(replacementNetwork.getNeighbours(node),replacementNetwork);
                break;
            case RESOURCE_ALLOCATION:
                sf = new ResourceAllocation(replacementNetwork.getNeighbours(node),replacementNetwork);
                break;
            case RANDOM_SIM:
                sf = new RandomSim(replacementNetwork.getNeighbours(node));
                break;
            case APRIORI:
                sf = new APriori(replacementNetwork.getNeighbours(node));
                break;
            case JACCARD:
            default:
                sf = new JaccardSimilarity(replacementNetwork.getNeighbours(node));
                break;
        }
        return sf;

    }
}
