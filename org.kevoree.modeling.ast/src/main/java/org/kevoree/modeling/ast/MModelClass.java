package org.kevoree.modeling.ast;

import java.util.*;

public class MModelClass extends MModelClassifier {

    public int globalIndex = 0;

    private Map<String, MModelAttribute> attributes = new HashMap<String, MModelAttribute>();
    private Map<String, MModelReference> references = new HashMap<String, MModelReference>();
    private Map<String, MModelClass> parents = new HashMap<String, MModelClass>();
    private Map<String, MModelOperation> operations = new HashMap<String, MModelOperation>();

    private Map<String, MModelDependency> dependencies = new HashMap<String, MModelDependency>();
    private Map<String, MModelInput> inputs = new HashMap<String, MModelInput>();
    private Map<String, MModelOutput> outputs = new HashMap<String, MModelOutput>();

    public MModelClass(String name) {
        this.name = name;
    }

    public boolean isInferred() {
        return this.inference != null;
    }

    private String inference = null;
    private Long temporalResolution = null;

    public Long getTemporalLimit() {
        return temporalLimit;
    }

    public void setTemporalLimit(Long temporalLimit) {
        this.temporalLimit = temporalLimit;
    }

    public Long getTemporalResolution() {
        return temporalResolution;
    }

    public void setTemporalResolution(Long temporalResolution) {
        this.temporalResolution = temporalResolution;
    }

    public String getInference() {
        return inference;
    }

    public void setInference(String inference) {
        this.inference = inference;
    }

    private Long temporalLimit = null;

    public void addAttribute(MModelAttribute att) {
        attributes.put(att.getName(), att);
    }

    public Collection<MModelAttribute> getAttributes() {
        HashMap<String, MModelAttribute> collected = new HashMap<String, MModelAttribute>();
        HashMap<String, MModelClass> passed = new HashMap<String, MModelClass>();
        deep_collect_atts(collected, passed);
        for (String collectedKey : collected.keySet()) {
            if (!attributes.containsKey(collectedKey)) {
                attributes.put(collectedKey, collected.get(collectedKey).clone());
            }
        }
        return attributes.values();
    }

    private void deep_collect_atts(HashMap<String, MModelAttribute> collector, HashMap<String, MModelClass> passed) {
        if (passed.containsKey(this.getName())) {
            return;
        } else {
            for (String key : attributes.keySet()) {
                if (!collector.containsKey(key)) {
                    collector.put(key, attributes.get(key));
                }
            }
            passed.put(getName(), this);
            for (MModelClass parent : getParents()) {
                parent.deep_collect_atts(collector, passed);
            }
        }
    }

    public void addReference(MModelReference ref) {
        references.put(ref.getName(), ref);
    }

    public void addDependency(MModelDependency el) {
        dependencies.put(el.getName(), el);
    }

    public MModelDependency[] getDependencies() {
        MModelDependency[] flat = dependencies.values().toArray(new MModelDependency[dependencies.size()]);
        Arrays.sort(flat, new Comparator<MModelDependency>() {
            @Override
            public int compare(MModelDependency o1, MModelDependency o2) {
                return o1.getIndex() - o2.getIndex();
            }
        });
        return flat;
    }

    public void addInput(MModelInput el) {
        inputs.put(el.getName(), el);
    }

    public Collection<MModelInput> getInputs() {
        return inputs.values();
    }

    public void addOutput(MModelOutput el) {
        outputs.put(el.getName(), el);
    }

    public MModelOutput[] getOutputs() {
        MModelOutput[] flat = outputs.values().toArray(new MModelOutput[outputs.size()]);
        Arrays.sort(flat, new Comparator<MModelOutput>() {
            @Override
            public int compare(MModelOutput o1, MModelOutput o2) {
                return o1.getIndex() - o2.getIndex();
            }
        });
        return flat;
    }

    public boolean multipleOutput() {
        return this.getOutputs().length > 1;
    }

    public Collection<MModelReference> getReferences() {
        HashMap<String, MModelReference> collected = new HashMap<String, MModelReference>();
        HashMap<String, MModelClass> passed = new HashMap<String, MModelClass>();
        deep_collect_refs(collected, passed);
        for (String collectedKey : collected.keySet()) {
            if (!references.containsKey(collectedKey)) {
                references.put(collectedKey, collected.get(collectedKey).clone());
            }
        }
        return references.values();
    }

    private void deep_collect_refs(HashMap<String, MModelReference> collector, HashMap<String, MModelClass> passed) {
        if (passed.containsKey(this.getName())) {
            return;
        } else {
            for (String key : references.keySet()) {
                if (!collector.containsKey(key)) {
                    collector.put(key, references.get(key));
                }
            }
            passed.put(getName(), this);
            for (MModelClass parent : getParents()) {
                parent.deep_collect_refs(collector, passed);
            }
        }
    }

    public void addParent(MModelClass cls) {
        parents.put(cls.getName(), cls);
    }

    public Collection<MModelClass> getParents() {
        return parents.values();
    }

    public Collection<MModelOperation> getOperations() {
        HashMap<String, MModelOperation> collected = new HashMap<String, MModelOperation>();
        HashMap<String, MModelClass> passed = new HashMap<String, MModelClass>();
        deep_collect_ops(collected, passed);
        for (String collectedKey : collected.keySet()) {
            if (!operations.containsKey(collectedKey)) {
                operations.put(collectedKey, collected.get(collectedKey).clone());
            }
        }
        return operations.values();
    }

    private void deep_collect_ops(HashMap<String, MModelOperation> collector, HashMap<String, MModelClass> passed) {
        if (passed.containsKey(this.getName())) {
            return;
        } else {
            for (String key : operations.keySet()) {
                if (!collector.containsKey(key)) {
                    collector.put(key, operations.get(key));
                }
            }
            passed.put(getName(), this);
            for (MModelClass parent : getParents()) {
                parent.deep_collect_ops(collector, passed);
            }
        }
    }

    public void addOperation(MModelOperation operation) {
        this.operations.put(operation.getName(), operation);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Class[name:");
        sb.append(getName());
        sb.append(", package:");
        sb.append(getPack());
        sb.append(", parent:");
        sb.append(getParents());
        sb.append(", attributes[\n");
        for (MModelAttribute att : attributes.values()) {
            sb.append(att.getName());
            sb.append(":");
            sb.append(att.getType());
            sb.append(",\n");
        }
        sb.append("]");
        sb.append(", references:[\n");
        for (MModelReference att : references.values()) {
            sb.append(att.getName());
            sb.append(":");
            sb.append(att.getType().getName());
            if (att.getOpposite() != null) {
                sb.append(" opposite ");
                sb.append(att.getOpposite());
            }
            sb.append(",\n");
        }
        sb.append("]]\n");
        return sb.toString();
    }
}
