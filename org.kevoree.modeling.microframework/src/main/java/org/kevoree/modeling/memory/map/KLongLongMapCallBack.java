package org.kevoree.modeling.memory.map;

/**
 * Created by duke on 04/03/15.
 */
public interface KLongLongMapCallBack<V> {

    public void on(long key, long value);

}
