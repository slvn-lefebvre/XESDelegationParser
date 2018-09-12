package org.isep.sociallog.similarity;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

import java.util.Map;


/**
 * Created by slefebvr on 20/03/17.
 */
public abstract class SimilarityFun implements Maps.EntryTransformer<String,Map<String, Double>, Double> {
    protected final Map<String, Double> node;


    public SimilarityFun(Map<String, Double> node) {
        this.node = node;
    }

}
