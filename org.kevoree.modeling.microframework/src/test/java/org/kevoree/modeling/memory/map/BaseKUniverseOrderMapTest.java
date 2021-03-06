package org.kevoree.modeling.memory.map;

import org.junit.Assert;
import org.junit.Test;
import org.kevoree.modeling.KConfig;

public abstract class BaseKUniverseOrderMapTest {

    private static final int SIZE = 100;

    public abstract KUniverseOrderMap createKUniverseOrderMap();

    @Test
    public void test() {
        KLongLongMap map = createKUniverseOrderMap();//"org.kevoree.modeling.Hello");
        for (long i = 0; i < SIZE; i++) {
            map.put(i, i);
        }
        Assert.assertEquals(map.size(), SIZE);
        for (long i = 0; i < SIZE; i++) {
            Assert.assertEquals(i, map.get(i));
        }
        final int[] nbCall = {0};
        map.each(new KLongLongMapCallBack() {
            @Override
            public void on(long key, long s) {
                nbCall[0]++;
                Assert.assertEquals(key, s);
            }
        });
        Assert.assertEquals(nbCall[0], SIZE);
        map.clear();
        Assert.assertEquals(map.size(), 0);
    }

    @Test
    public void testLoadSave() throws Exception {
        KUniverseOrderMap map = createKUniverseOrderMap();//, "org.kevoree.modeling.Hello");
        for (long i = 0; i < 10; i++) {
            map.put(i, i);
        }
        Assert.assertEquals(map.size(), 10);
        String saved = map.serialize(null);

        Assert.assertEquals(saved, "org.kevoree.modeling.Hello,U/A:A,C:C,E:E,G:G,I:I,K:K,M:M,O:O,Q:Q,S:S");

        KUniverseOrderMap map2 = createKUniverseOrderMap();//, null);
        map2.init(saved, null, -1);
        Assert.assertEquals(map2.size(), 10);
        for (long i = 0; i < 10; i++) {
            Assert.assertEquals(map.get(i), i);
            Assert.assertEquals(map2.get(i), i);
        }

        Assert.assertEquals(map.size(), map2.size());
        Assert.assertEquals(map.metaClassName(), map2.metaClassName());

        String saved2 = map2.serialize(null);

        Assert.assertEquals(saved2, "org.kevoree.modeling.Hello,U/A:A,C:C,E:E,G:G,I:I,K:K,M:M,O:O,Q:Q,S:S");

        KUniverseOrderMap nullClassName = createKUniverseOrderMap();//, null);
        for (long i = 0; i < 10; i++) {
            nullClassName.put(i, i);
        }
        String nullSaved = nullClassName.serialize(null);
        Assert.assertEquals(nullSaved, "U/A:A,C:C,E:E,G:G,I:I,K:K,M:M,O:O,Q:Q,S:S");
        KUniverseOrderMap mapNull2 = createKUniverseOrderMap();//, null);
        mapNull2.init(nullSaved, null, -1);
        String nullSaved2 = mapNull2.serialize(null);
        Assert.assertEquals(nullSaved2, "U/A:A,C:C,E:E,G:G,I:I,K:K,M:M,O:O,Q:Q,S:S");

    }

    @Test
    public void testInitThenPut() {
        KUniverseOrderMap map = createKUniverseOrderMap();//, "org.kevoree.modeling.Hello");
        map.init(null, null, -1);
        map.put(0, 0);
        map.put(1, 1);
    }

}
