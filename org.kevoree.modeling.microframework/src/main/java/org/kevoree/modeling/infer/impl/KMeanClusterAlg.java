package org.kevoree.modeling.infer.impl;

import org.kevoree.modeling.KObject;
import org.kevoree.modeling.abs.AbstractKObject;
import org.kevoree.modeling.infer.KInferAlg;
import org.kevoree.modeling.memory.manager.internal.KInternalDataManager;
import org.kevoree.modeling.memory.chunk.KMemoryChunk;
import org.kevoree.modeling.util.maths.structure.impl.Array1D;

import java.util.Random;

public class KMeanClusterAlg implements KInferAlg {

    //TODO to replace by meta-learning parameters
    private int k = 3; //number of clusters
    private int iterations = 100;

    @Override
    public void train(double[][] trainingSet, double[][] expectedResultSet, KObject origin, KInternalDataManager manager) {
        if (trainingSet.length < k) {
            throw new RuntimeException("training setPrimitiveType not enough");
        }
        KMemoryChunk ks = manager.preciseChunk(origin.universe(), origin.now(), origin.uuid(), origin.metaClass(), ((AbstractKObject) origin).previousResolved());
        int dependenciesIndex = origin.metaClass().dependencies().index();
        //Create initial chunk if empty
        int size = k * origin.metaClass().inputs().length;
        if (ks.getDoubleArraySize(dependenciesIndex, origin.metaClass()) == 0) {
            ks.extendDoubleArray(origin.metaClass().dependencies().index(), size, origin.metaClass());

            //Start by selecting first K points as centroids
            for (int i = 0; i < k; i++) {
                for (int j = 0; j < origin.metaClass().inputs().length; j++) {
                    ks.setDoubleArrayElem(dependenciesIndex, j + i * origin.metaClass().inputs().length, trainingSet[i][j], origin.metaClass());
                }
            }
        }
        Array1D state = new Array1D(size, 0, origin.metaClass().dependencies().index(), ks, origin.metaClass());

        for (int iter = 0; iter < iterations; iter++) {
            int temporalClassification;
            double[][] centroids = new double[k][origin.metaClass().inputs().length];
            int[] counters = new int[k];

            for (int j = 0; j < k; j++) {
                centroids[j] = new double[origin.metaClass().inputs().length];
                counters[j] = 0;
            }

            for (int i = 0; i < trainingSet.length; i++) {
                //Step 1, classify according to current centroids
                temporalClassification = classify(trainingSet[i], state);

                //Step 2 update the centroids in live
                for (int j = 0; j < origin.metaClass().inputs().length; j++) {
                    centroids[temporalClassification][j] += trainingSet[i][j];
                }
                counters[temporalClassification]++;
            }

            //Step 3 replace the current state by the new centroids
            for (int i = 0; i < k; i++) {
                if (counters[i] != 0) {
                    for (int j = 0; j < origin.metaClass().inputs().length; j++) {
                        state.set(j + i * origin.metaClass().inputs().length, centroids[i][j] / counters[i]);
                    }
                } else {
                    Random rand = new Random();
                    int pos = rand.nextInt(trainingSet.length);
                    for (int j = 0; j < origin.metaClass().inputs().length; j++) {
                        state.set(j + i * origin.metaClass().inputs().length, trainingSet[pos][j]);
                    }
                }
            }
        }
    }

    private int classify(double[] features, Array1D state) {
        double maxdistance = -1;
        int classNum = -1;
        for (int i = 0; i < k; i++) {
            double currentdist = 0;
            for (int j = 0; j < features.length; j++) {
                currentdist += (features[j] - state.get(i * features.length + j)) * (features[j] - state.get(i * features.length + j));
            }
            if (maxdistance < 0) {
                maxdistance = currentdist;
                classNum = i;
            } else {
                if (currentdist < maxdistance) {
                    maxdistance = currentdist;
                    classNum = i;
                }
            }
        }
        return classNum;
    }

    @Override
    public double[][] infer(double[][] features, KObject origin, KInternalDataManager manager) {
        KMemoryChunk ks = manager.closestChunk(origin.universe(), origin.now(), origin.uuid(), origin.metaClass(), ((AbstractKObject) origin).previousResolved());
        int dependenciesIndex = origin.metaClass().dependencies().index();
        int size = k * origin.metaClass().inputs().length;
        if (ks.getDoubleArraySize(dependenciesIndex, origin.metaClass()) == 0) {
            return null;
        }
        Array1D state = new Array1D(size, 0, origin.metaClass().dependencies().index(), ks, origin.metaClass());

        double[][] result = new double[features.length][1];

        for (int inst = 0; inst < features.length; inst++) {
            result[inst] = new double[1];
            result[inst][0] = classify(features[inst], state);
        }
        return result;
    }
}
