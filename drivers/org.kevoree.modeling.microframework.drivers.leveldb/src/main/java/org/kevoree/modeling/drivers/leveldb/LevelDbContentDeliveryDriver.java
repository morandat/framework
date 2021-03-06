package org.kevoree.modeling.drivers.leveldb;

import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.kevoree.modeling.*;
import org.kevoree.modeling.KContentKey;
import org.kevoree.modeling.cdn.KContentDeliveryDriver;
import org.kevoree.modeling.cdn.KContentUpdateListener;
import org.kevoree.modeling.memory.map.KIntMapCallBack;
import org.kevoree.modeling.memory.map.impl.ArrayIntMap;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LevelDbContentDeliveryDriver implements KContentDeliveryDriver {

    private Options options = new Options().createIfMissing(true);

    private DB db;

    private final Lock lock = new ReentrantLock();

    private String _storagePath = null;

    private boolean _isConnected = false;

    @Override
    public void connect(KCallback<Throwable> callback) {
        File location = new File(_storagePath);
        if (!location.exists()) {
            location.mkdirs();
        }
        File targetDB = new File(location, "data");
        Exception exception = null;
        try {
            db = JniDBFactory.factory.open(targetDB, options);
            _isConnected = true;
        } catch (Exception e) {
            exception = e;
        }
        //noop
        if (callback != null) {
            callback.on(exception);
        }
    }

    public LevelDbContentDeliveryDriver(String p_storagePath) throws IOException {
        this._storagePath = p_storagePath;
    }

    private String _connectedError = "PLEASE CONNECT YOUR DATABASE FIRST";

    @Override
    public void atomicGetIncrement(KContentKey key, KCallback<Short> cb) {
        String result = JniDBFactory.asString(db.get(JniDBFactory.bytes(key.toString())));
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
        WriteBatch batch = db.createWriteBatch();
        batch.put(JniDBFactory.bytes(key.toString()), JniDBFactory.bytes(nextV + ""));
        db.write(batch);
        cb.on(previousV);
    }

    @Override
    public void get(KContentKey[] keys, KCallback<String[]> callback) {
        if (!_isConnected) {
            throw new RuntimeException(_connectedError);
        }
        String[] result = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            result[i] = JniDBFactory.asString(db.get(JniDBFactory.bytes(keys[i].toString())));
        }
        if (callback != null) {
            callback.on(result);
        }
    }

    @Override
    public synchronized void put(KContentKey[] p_keys, String[] p_values, KCallback<Throwable> p_callback, int excludeListener) {
        if (!_isConnected) {
            throw new RuntimeException(_connectedError);
        }
        WriteBatch batch = db.createWriteBatch();
        for (int i = 0; i < p_keys.length; i++) {
            batch.put(JniDBFactory.bytes(p_keys[i].toString()), JniDBFactory.bytes(p_values[i]));
        }
        db.write(batch);
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

    @Override
    public void remove(String[] keys, KCallback<Throwable> error) {
        if (!_isConnected) {
            throw new RuntimeException(_connectedError);
        }
        try {
            for (int i = 0; i < keys.length; i++) {
                db.delete(JniDBFactory.bytes(keys[i]));
            }
            if (error != null) {
                error.on(null);
            }
        } catch (Exception e) {
            if (error != null) {
                error.on(e);
            }
        }
    }

    @Override
    public void close(KCallback<Throwable> error) {
        db.write(db.createWriteBatch());
        try {
            db.close();
            _isConnected = false;
            if (error != null) {
                error.on(null);
            }
        } catch (IOException e) {
            if (error != null) {
                error.on(e);
            }
        }
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
    private int randomInterceptorID() {
        return random.nextInt();
    }

    @Override
    public synchronized int addUpdateListener(KContentUpdateListener p_interceptor) {
        if (additionalInterceptors == null) {
            additionalInterceptors = new ArrayIntMap<KContentUpdateListener>(KConfig.CACHE_INIT_SIZE, KConfig.CACHE_LOAD_FACTOR);
        }
        int newID = randomInterceptorID();
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
