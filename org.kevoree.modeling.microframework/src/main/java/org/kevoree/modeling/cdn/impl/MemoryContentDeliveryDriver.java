package org.kevoree.modeling.cdn.impl;

import org.kevoree.modeling.KCallback;
import org.kevoree.modeling.KConfig;
import org.kevoree.modeling.cdn.KContentUpdateListener;
import org.kevoree.modeling.KContentKey;
import org.kevoree.modeling.cdn.KContentDeliveryDriver;
import org.kevoree.modeling.memory.map.KIntMapCallBack;
import org.kevoree.modeling.memory.map.KStringMap;
import org.kevoree.modeling.memory.map.impl.ArrayIntMap;
import org.kevoree.modeling.memory.map.impl.ArrayStringMap;

import java.util.Random;

public class MemoryContentDeliveryDriver implements KContentDeliveryDriver {

    private final KStringMap<String> backend = new ArrayStringMap<String>(KConfig.CACHE_INIT_SIZE, KConfig.CACHE_LOAD_FACTOR);

    @Override
    public void atomicGetIncrement(long[] key, KCallback<Short> cb) {
        String result = backend.get(KContentKey.toString(key, 0));
        short nextV;
        short previousV;
        if (result != null) {
            try {
                previousV = Short.parseShort(result);
            } catch (Exception e) {
                e.printStackTrace();
                previousV = Short.MIN_VALUE;
            }
        } else {
            previousV = 0;
        }
        if (previousV == Short.MAX_VALUE) {
            nextV = Short.MIN_VALUE;
        } else {
            nextV = (short) (previousV + 1);
        }
        backend.put(KContentKey.toString(key, 0), "" + nextV);
        cb.on(previousV);
    }

    @Override
    public void get(long[] keys, KCallback<String[]> callback) {
        int nbKeys = keys.length / 3;
        String[] values = new String[nbKeys];
        for (int i = 0; i < nbKeys; i++) {
            values[i] = backend.get(KContentKey.toString(keys, i));
        }
        if (callback != null) {
            callback.on(values);
        }
    }

    @Override
    public synchronized void put(long[] p_keys, String[] p_values, KCallback<Throwable> p_callback, int excludeListener) {
        if (p_keys.length != p_values.length) {
            p_callback.on(new Exception("Bad Put Usage !"));
        } else {
            int nbKeys = p_keys.length / 3;
            for (int i = 0; i < nbKeys; i++) {
                backend.put(KContentKey.toString(p_keys, i), p_values[i]);
            }
            if (additionalInterceptors != null) {
                additionalInterceptors.each(new KIntMapCallBack<KContentUpdateListener>() {
                    @Override
                    public void on(int key, KContentUpdateListener value) {
                        if (value != null && key != excludeListener) {
                            value.on(p_keys);
                        }
                    }
                });
            }
            if (p_callback != null) {
                p_callback.on(null);
            }
        }
    }

    @Override
    public void remove(String[] keys, KCallback<Throwable> callback) {
        for (int i = 0; i < keys.length; i++) {
            backend.remove(keys[i]);
        }
        if (callback != null) {
            callback.on(null);
        }
    }

    @Override
    public void connect(KCallback<Throwable> callback) {
        if (callback != null) {
            callback.on(null);
        }
    }

    @Override
    public void close(KCallback<Throwable> callback) {
        backend.clear();
        callback.on(null);
    }

    private ArrayIntMap<KContentUpdateListener> additionalInterceptors = null;

    /**
     * @ignore ts
     */
    private Random random = new Random();

    /**
     * @native ts
     * return Math.random();
     */
    private int nextListenerID() {
        return random.nextInt();
    }

    @Override
    public synchronized int addUpdateListener(KContentUpdateListener p_interceptor) {
        if (additionalInterceptors == null) {
            additionalInterceptors = new ArrayIntMap<KContentUpdateListener>(KConfig.CACHE_INIT_SIZE, KConfig.CACHE_LOAD_FACTOR);
        }
        int newID = nextListenerID();
        additionalInterceptors.put(newID, p_interceptor);
        return newID;
    }

    @Override
    public synchronized void removeUpdateListener(int id) {
        if (additionalInterceptors != null) {
            additionalInterceptors.remove(id);
        }
    }

}