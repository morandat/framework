package org.kevoree.modeling.util.maths.structure;

public interface KArray1D {

    int size();

    double get(int index);

    double set(int index, double value);

    double add (int index, double value);
}
