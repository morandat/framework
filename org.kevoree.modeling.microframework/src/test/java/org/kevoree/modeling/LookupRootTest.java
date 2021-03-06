package org.kevoree.modeling;

import org.junit.Assert;
import org.junit.Test;
import org.kevoree.modeling.cdn.impl.MemoryContentDeliveryDriver;
import org.kevoree.modeling.cloudmodel.*;
import org.kevoree.modeling.memory.manager.DataManagerBuilder;

public class LookupRootTest {

    @Test
    public void loadRootFromDbTest() {
//        MemoryKDataBase.DEBUG = true;

        final CloudModel cloudModel = new CloudModel(DataManagerBuilder.buildDefault());
        cloudModel.connect(null);

        final CloudUniverse dimension0 = cloudModel.newUniverse();
        final CloudView t0 = dimension0.time(0l);

        // create node0 and element0 and link them
        final Node node0 = t0.createNode();
        final Element element0 = t0.createElement();
        node0.setElement(element0);

        t0.setRoot(node0, new KCallback<Throwable>() {
            @Override

            public void on(Throwable throwable) {

                cloudModel.save(new KCallback<Throwable>() {
                    @Override
                    public void on(Throwable aBoolean) {
                        cloudModel.discard(new KCallback<Throwable>() {
                            @Override
                            public void on(Throwable aBoolean) {

                            }
                        });
                    }
                });

                final CloudView lookupView = dimension0.time(0l);
                cloudModel.manager().getRoot(lookupView.universe(), lookupView.now(), new KCallback<KObject>() {
                    @Override
                    public void on(KObject kObject) {
                        Assert.assertNotNull(kObject);
                    }
                });

                lookupView.select("@root", new KCallback<Object[]>() {
                    @Override
                    public void on(Object[] kObjects) {
                        Assert.assertNotNull(kObjects[0]);
                    }
                });
            }
        });
    }


    @Test
    public void reloadRootFromDbTest() {
        final MemoryContentDeliveryDriver db = new MemoryContentDeliveryDriver();

        final CloudModel cloudModel = new CloudModel(DataManagerBuilder.create().withContentDeliveryDriver(db).build());
        cloudModel.connect(null);

        final CloudUniverse dimension0 = cloudModel.newUniverse();
        final CloudView t0 = dimension0.time(0l);

        // create node0 and element0 and link them
        final Node node0 = t0.createNode();
        final Element element0 = t0.createElement();
        node0.setElement(element0);


        t0.setRoot(node0, new KCallback<Throwable>() {
            @Override
            public void on(Throwable throwable) {
                cloudModel.save(new KCallback<Throwable>() {
                    @Override
                    public void on(Throwable aBoolean) {
                        cloudModel.discard(new KCallback<Throwable>() {
                            @Override
                            public void on(Throwable aBoolean) {

                            }
                        });
                    }
                });
            }
        });

        final CloudModel universe1 = new CloudModel(DataManagerBuilder.create().withContentDeliveryDriver(db).build());
        universe1.connect(null);
        final CloudUniverse cloudDimension1 = universe1.universe(dimension0.key());
        final CloudView cloudView1 = cloudDimension1.time(1l);

        cloudView1.select("@root", new KCallback<Object[]>() {
            @Override
            public void on(Object[] kObjects) {
                Assert.assertNotNull(kObjects[0]);
            }
        });

    }
}
