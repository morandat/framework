package org.kevoree.modeling.traversal;

import org.kevoree.modeling.KObject;
import org.kevoree.modeling.KCallback;
import org.kevoree.modeling.meta.KMetaAttribute;
import org.kevoree.modeling.meta.KMetaReference;

public interface KTraversal {

    KTraversal traverse(KMetaReference metaReference);

    KTraversal traverseQuery(String metaReferenceQuery);

    KTraversal attributeQuery(String attributeQuery);

    KTraversal withAttribute(KMetaAttribute attribute, Object expectedValue);

    KTraversal withoutAttribute(KMetaAttribute attribute, Object expectedValue);

    KTraversal filter(KTraversalFilter filter);

    void then(KCallback<KObject[]> cb);

    void eval(String expression, KCallback<Object[]> callback);

    void map(KMetaAttribute attribute, KCallback<Object[]> cb);

    KTraversal collect(KMetaReference metaReference, KTraversalFilter continueCondition);

    KTraversal traverseTime(long timeOffset, long steps, KTraversalFilter continueCondition);

    KTraversal traverseUniverse(long universeOffset, KTraversalFilter continueCondition);

    KTraversal traverseIndex(String indexName);

    void exec(KObject[] origins, KTraversalIndexResolver resolver, KCallback<Object[]> callback);

}


