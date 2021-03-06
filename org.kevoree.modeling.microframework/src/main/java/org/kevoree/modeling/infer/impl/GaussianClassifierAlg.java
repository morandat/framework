package org.kevoree.modeling.infer.impl;

import org.kevoree.modeling.KObject;
import org.kevoree.modeling.abs.AbstractKObject;
import org.kevoree.modeling.infer.KInferAlg;
import org.kevoree.modeling.memory.manager.internal.KInternalDataManager;
import org.kevoree.modeling.memory.chunk.KMemoryChunk;
import org.kevoree.modeling.meta.KMetaDependencies;
import org.kevoree.modeling.meta.impl.MetaEnum;
import org.kevoree.modeling.util.maths.Distribution;
import org.kevoree.modeling.util.maths.structure.impl.Array1D;

/**
 * Created by assaad on 08/07/15.
 */
public class GaussianClassifierAlg implements KInferAlg {

    private static int MIN = 0;
    private static int MAX = 1;
    private static int SUM = 2;
    private static int SUMSQUARE = 3;
    //to keep updated
    private static int NUMOFFIELDS = 4;


    private int getIndex(int input, int output, int field, KMetaDependencies meta) {
        return output * (NUMOFFIELDS * meta.origin().inputs().length + 1) + NUMOFFIELDS * input + field;
    }

    private int getCounter(int output, KMetaDependencies meta) {
        return output * (NUMOFFIELDS * meta.origin().inputs().length + 1) + NUMOFFIELDS * meta.origin().inputs().length;
    }


    public double[] getAvg(int output, Array1D state, KMetaDependencies meta) {
        double[] avg = new double[meta.origin().inputs().length];
        double total = state.get(getCounter(output, meta));
        if (total != 0) {
            for (int i = 0; i < meta.origin().inputs().length; i++) {
                avg[i] = state.get(getIndex(i, output, SUM, meta)) / total;
            }
        }
        return avg;
    }

    public double[] getVariance(int output, Array1D state, double[] avg, KMetaDependencies meta) {
        double[] variances = new double[meta.origin().inputs().length];
        double total = state.get(getCounter(output, meta));
        if (total != 0) {
            for (int i = 0; i < meta.origin().inputs().length; i++) {
                variances[i] = state.get(getIndex(i, output, SUMSQUARE, meta)) / total - avg[i] * avg[i]; // x count/ (count-1)
            }
        }
        return variances;
    }

    @Override
    public void train(double[][] trainingSet, double[][] expectedResultSet, KObject origin, KInternalDataManager manager) {
        int maxOutput = ((MetaEnum) origin.metaClass().outputs()[0].type()).literals().length;
        KMemoryChunk ks = manager.preciseChunk(origin.universe(), origin.now(), origin.uuid(), origin.metaClass(), ((AbstractKObject) origin).previousResolved());
        int dependenciesIndex = origin.metaClass().dependencies().index();
        //Create initial chunk if empty
        int size = (maxOutput + 1) * (origin.metaClass().inputs().length * NUMOFFIELDS + 1);
        if (ks.getDoubleArraySize(dependenciesIndex, origin.metaClass()) == 0) {
            ks.extendDoubleArray(origin.metaClass().dependencies().index(), size, origin.metaClass());
            for (int i = 0; i < size; i++) {
                ks.setDoubleArrayElem(dependenciesIndex, i, 0, origin.metaClass());
            }
        }

        Array1D state = new Array1D(size, 0, origin.metaClass().dependencies().index(), ks, origin.metaClass());

        //update the state
        for (int i = 0; i < trainingSet.length; i++) {
            int output = (int) expectedResultSet[i][0];
            for (int j = 0; j < origin.metaClass().inputs().length; j++) {
                //If this is the first datapoint
                if (state.get(getCounter(output, origin.metaClass().dependencies())) == 0) {
                    state.set(getIndex(j, output, MIN, origin.metaClass().dependencies()), trainingSet[i][j]);
                    state.set(getIndex(j, output, MAX, origin.metaClass().dependencies()), trainingSet[i][j]);
                    state.set(getIndex(j, output, SUM, origin.metaClass().dependencies()), trainingSet[i][j]);
                    state.set(getIndex(j, output, SUMSQUARE, origin.metaClass().dependencies()), trainingSet[i][j] * trainingSet[i][j]);

                } else {
                    if (trainingSet[i][j] < state.get(getIndex(j, output, MIN, origin.metaClass().dependencies()))) {
                        state.set(getIndex(j, output, MIN, origin.metaClass().dependencies()), trainingSet[i][j]);
                    }
                    if (trainingSet[i][j] > state.get(getIndex(j, output, MAX, origin.metaClass().dependencies()))) {
                        state.set(getIndex(j, output, MAX, origin.metaClass().dependencies()), trainingSet[i][j]);
                    }
                    state.add(getIndex(j, output, SUM, origin.metaClass().dependencies()), trainingSet[i][j]);
                    state.add(getIndex(j, output, SUMSQUARE, origin.metaClass().dependencies()), trainingSet[i][j] * trainingSet[i][j]);
                }

                //update global stat
                if (state.get(getCounter(maxOutput, origin.metaClass().dependencies())) == 0) {
                    state.set(getIndex(j, maxOutput, MIN, origin.metaClass().dependencies()), trainingSet[i][j]);
                    state.set(getIndex(j, maxOutput, MAX, origin.metaClass().dependencies()), trainingSet[i][j]);
                    state.set(getIndex(j, maxOutput, SUM, origin.metaClass().dependencies()), trainingSet[i][j]);
                    state.set(getIndex(j, maxOutput, SUMSQUARE, origin.metaClass().dependencies()), trainingSet[i][j] * trainingSet[i][j]);
                } else {
                    if (trainingSet[i][j] < state.get(getIndex(j, maxOutput, MIN, origin.metaClass().dependencies()))) {
                        state.set(getIndex(j, maxOutput, MIN, origin.metaClass().dependencies()), trainingSet[i][j]);
                    }
                    if (trainingSet[i][j] > state.get(getIndex(j, maxOutput, MAX, origin.metaClass().dependencies()))) {
                        state.set(getIndex(j, maxOutput, MAX, origin.metaClass().dependencies()), trainingSet[i][j]);
                    }
                    state.add(getIndex(j, maxOutput, SUM, origin.metaClass().dependencies()), trainingSet[i][j]);
                    state.add(getIndex(j, maxOutput, SUMSQUARE, origin.metaClass().dependencies()), trainingSet[i][j] * trainingSet[i][j]);
                }
            }

            //Update Global counters
            state.add(getCounter(output, origin.metaClass().dependencies()), 1);
            state.add(getCounter(maxOutput, origin.metaClass().dependencies()), 1);
        }
    }


    @Override
    public double[][] infer(double[][] features, KObject origin, KInternalDataManager manager) {
        int maxOutput = ((MetaEnum) origin.metaClass().outputs()[0].type()).literals().length;
        KMemoryChunk ks = manager.closestChunk(origin.universe(), origin.now(), origin.uuid(), origin.metaClass(), ((AbstractKObject) origin).previousResolved());
        int dependenciesIndex = origin.metaClass().dependencies().index();
        //check if chunk is empty
        int size = (maxOutput + 1) * (origin.metaClass().inputs().length * NUMOFFIELDS + 1);
        if (ks.getDoubleArraySize(dependenciesIndex, origin.metaClass()) == 0) {
            return null;
        }
        Array1D state = new Array1D(size, 0, origin.metaClass().dependencies().index(), ks, origin.metaClass());
        double[][] result = new double[features.length][1];

        for (int j = 0; j < features.length; j++) {
            result[j] = new double[1];
            double maxprob = 0;
            double prob = 0;
            for (int output = 0; output < maxOutput; output++) {
                prob = getProba(features[j], output, state, origin.metaClass().dependencies());
                if (prob > maxprob) {
                    maxprob = prob;
                    result[j][0] = output;
                }
            }
        }
        return result;
    }

    public double getProba(double[] features, int output, Array1D state, KMetaDependencies meta) {
        double prob = 0;
        double[] avg = getAvg(output, state, meta);
        double[] variance = getVariance(output, state, avg, meta);
        prob = Distribution.gaussian(features, avg, variance);
        return prob;
    }

    public double[] getAllProba(double[] features, Array1D state, KMetaDependencies meta, int maxOutput) {
        double[] results = new double[maxOutput];
        for (int i = 0; i < maxOutput; i++) {
            results[i] = getProba(features, i, state, meta);
        }
        return results;
    }
}
