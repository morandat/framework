package org.kevoree.modeling.memory.map;

public interface KIntMap<V> {

    boolean contains(int key);

    V get(int key);

    void put(int key, V value);

    void each(KIntMapCallBack<V> callback);

}
