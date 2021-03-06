package org.kevoree.modeling.ast;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class MModel {

    private String version = null;

    private String kmfVersion = null;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getKmfVersion() {
        return kmfVersion;
    }

    public void setKmfVersion(String kmfVersion) {
        this.kmfVersion = kmfVersion;
    }

    private HashMap<String, MModelClassifier> classifiers = new HashMap<>();

    private Integer classIndex = 0;
    private Integer enumIndex = 0;

    public Collection<MModelClassifier> getClassifiers() {
        return classifiers.values();
    }

    public MModelClassifier get(String fqn) {
        return classifiers.get(fqn);
    }

    public void addClassifier(MModelClassifier classifier) {
        if (classifier instanceof MModelClass) {
            classifier.setIndex(classIndex);
            classIndex = classIndex + 1;
        } else if (classifier instanceof MModelEnum) {
            classifier.setIndex(enumIndex);
            enumIndex = enumIndex + 1;
        } else {

        }
        classifiers.put(classifier.getFqn(), classifier);
    }

    public Collection<MModelClass> getClasses() {
        ArrayList<MModelClass> classes = new ArrayList<MModelClass>();
        for (MModelClassifier cls : classifiers.values()) {
            if (cls instanceof MModelClass) {
                classes.add((MModelClass) cls);
            }
        }
        return classes;
    }

    private final static Character ESCAPE_CHAR = '\\';

    private static String cleanString(String in) {
        String cleaned = in.substring(1, in.length() - 1);
        if (cleaned.length() == 0) {
            return cleaned;
        }
        StringBuilder builder = null;
        int i = 0;
        while (i < cleaned.length()) {
            Character current = cleaned.charAt(i);
            if (current == ESCAPE_CHAR) {
                if (builder == null) {
                    builder = new StringBuilder();
                    builder.append(cleaned.substring(0, i));
                }
                i++;
                Character current2 = cleaned.charAt(i);
                switch (current2) {
                    case '"':
                        builder.append('\"');
                        break;
                    case '\\':
                        builder.append(current2);
                        break;
                    case '/':
                        builder.append(current2);
                        break;
                    case 'b':
                        builder.append('\b');
                        break;
                    case 'f':
                        builder.append('\f');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case '{':
                        builder.append("\\{");
                        break;
                    case '}':
                        builder.append("\\}");
                        break;
                    case '[':
                        builder.append("\\[");
                        break;
                    case ']':
                        builder.append("\\]");
                        break;
                    case ',':
                        builder.append("\\,");
                        break;
                }
            } else {
                if (builder != null) {
                    builder = builder.append(current);
                }
            }
            i++;
        }
        if (builder != null) {
            return builder.toString();
        } else {
            return cleaned;
        }
    }

    public static MModel build(File mmFile) throws IOException {
        MModel model = new MModel();
        ANTLRFileStream fileStream = new ANTLRFileStream(mmFile.getAbsolutePath());
        BufferedTokenStream tokens = new CommonTokenStream(new org.kevoree.modeling.ast.MetaModelLexer(fileStream));
        org.kevoree.modeling.ast.MetaModelParser parser = new org.kevoree.modeling.ast.MetaModelParser(tokens);
        org.kevoree.modeling.ast.MetaModelParser.MetamodelContext mmctx = parser.metamodel();
        //first we do version
        for (org.kevoree.modeling.ast.MetaModelParser.AnnotationDeclrContext annotationDeclrContext : mmctx.annotationDeclr()) {
            if (annotationDeclrContext.IDENT().getText().toLowerCase().equals("version") && annotationDeclrContext.STRING() != null) {
                model.setVersion(cleanString(annotationDeclrContext.STRING().getText()));
            }
            if (annotationDeclrContext.IDENT().getText().toLowerCase().equals("kmfversion") && annotationDeclrContext.STRING() != null) {
                model.setKmfVersion(cleanString(annotationDeclrContext.STRING().getText()));
            }
        }
        //Second we do enum mapping
        for (org.kevoree.modeling.ast.MetaModelParser.DeclContext decl : mmctx.decl()) {
            if (decl.enumDeclr() != null) {
                org.kevoree.modeling.ast.MetaModelParser.EnumDeclrContext enumDeclrContext = decl.enumDeclr();
                String enumFqn = enumDeclrContext.TYPE_NAME().getText();
                final MModelEnum enumClass = model.getOrAddEnum(enumFqn);
                for (TerminalNode literal : enumDeclrContext.IDENT()) {
                    enumClass.addLitteral(literal.getText());
                }
            }
        }
        //thirdly we do classDeclr
        for (org.kevoree.modeling.ast.MetaModelParser.DeclContext decl : mmctx.decl()) {
            if (decl.classDeclr() != null) {
                org.kevoree.modeling.ast.MetaModelParser.ClassDeclrContext classDeclrContext = decl.classDeclr();
                String classFqn = classDeclrContext.TYPE_NAME().getText();
                final MModelClass newClass = model.getOrAddClass(classFqn);
                for (org.kevoree.modeling.ast.MetaModelParser.AttributeDeclarationContext attDecl : classDeclrContext.attributeDeclaration()) {
                    String name = attDecl.IDENT().getText();
                    org.kevoree.modeling.ast.MetaModelParser.AttributeTypeContext attType = attDecl.attributeType();
                    String value;
                    if (attType.TYPE_NAME() != null) {
                        value = attType.TYPE_NAME().getText();
                    } else {
                        value = attType.getText();
                    }
                    final MModelAttribute attribute = new MModelAttribute(name, value, newClass.globalIndex);
                    newClass.globalIndex++;
                    for (org.kevoree.modeling.ast.MetaModelParser.AnnotationDeclrContext annotDecl : attDecl.annotationDeclr()) {
                        if (annotDecl.IDENT().getText().toLowerCase().equals("precision") && annotDecl.NUMBER() != null) {
                            attribute.setPrecision(Double.parseDouble(annotDecl.NUMBER().getText()));
                        }
                    }
                    newClass.addAttribute(attribute);
                }
                for (org.kevoree.modeling.ast.MetaModelParser.ReferenceDeclarationContext refDecl : classDeclrContext.referenceDeclaration()) {
                    final MModelClass refType = model.getOrAddClass(refDecl.TYPE_NAME().getText());
                    MModelReference reference = new MModelReference(refDecl.IDENT().getText(), refType, newClass.globalIndex);
                    newClass.globalIndex++;
                    if (refDecl.getText().trim().startsWith("ref*")) {
                        reference.setSingle(false);
                    }
                    for (org.kevoree.modeling.ast.MetaModelParser.AnnotationDeclrContext annotDecl : refDecl.annotationDeclr()) {
                        if (annotDecl.IDENT().getText().toLowerCase().equals("opposite") && annotDecl.STRING() != null) {
                            reference.setOpposite(cleanString(annotDecl.STRING().getText()));
                            MModelReference oppRef = model.getOrAddReference(refType, reference.getOpposite(), newClass);
                            oppRef.setVisible(true);
                            oppRef.setOpposite(reference.getName());
                        }
                    }
                    newClass.addReference(reference);
                }
                for (org.kevoree.modeling.ast.MetaModelParser.DependencyDeclarationContext dependencyDeclarationContext : classDeclrContext.dependencyDeclaration()) {
                    final MModelClass depType = model.getOrAddClass(dependencyDeclarationContext.TYPE_NAME().getText());
                    MModelDependency dependency = new MModelDependency(dependencyDeclarationContext.IDENT().getText(), depType, newClass.globalIndex);
                    newClass.globalIndex++;
                    newClass.addDependency(dependency);
                }
                for (org.kevoree.modeling.ast.MetaModelParser.InputDeclarationContext inputDeclarationContext : classDeclrContext.inputDeclaration()) {
                    MModelInput input = new MModelInput(inputDeclarationContext.IDENT().getText(), cleanString(inputDeclarationContext.STRING().getText()), newClass.globalIndex);
                    newClass.globalIndex++;
                    newClass.addInput(input);
                }
                for (org.kevoree.modeling.ast.MetaModelParser.OutputDeclarationContext outputDeclarationContext : classDeclrContext.outputDeclaration()) {
                    org.kevoree.modeling.ast.MetaModelParser.AttributeTypeContext attType = outputDeclarationContext.attributeType();
                    String typeName;
                    if (attType.TYPE_NAME() != null) {
                        typeName = attType.TYPE_NAME().getText();
                    } else {
                        typeName = attType.getText();
                    }
                    MModelOutput output = new MModelOutput(outputDeclarationContext.IDENT().getText(), typeName, newClass.globalIndex);
                    newClass.globalIndex++;
                    newClass.addOutput(output);
                }
                if (classDeclrContext.classParentDeclr() != null) {
                    org.kevoree.modeling.ast.MetaModelParser.ClassParentDeclrContext parentDeclrContext = classDeclrContext.classParentDeclr();
                    for (TerminalNode tt : parentDeclrContext.TYPE_NAME()) {
                        final MModelClass newClassTT = model.getOrAddClass(tt.getText());
                        newClass.addParent(newClassTT);
                    }
                }
                for (org.kevoree.modeling.ast.MetaModelParser.AnnotationDeclrContext annotDecl : classDeclrContext.annotationDeclr()) {
                    if (annotDecl.IDENT().getText().toLowerCase().equals("temporalLimit") && annotDecl.NUMBER() != null) {
                        newClass.setTemporalLimit(Long.parseLong(annotDecl.NUMBER().getText()));
                    }
                    if (annotDecl.IDENT().getText().toLowerCase().equals("temporalResolution") && annotDecl.NUMBER() != null) {
                        newClass.setTemporalResolution(Long.parseLong(annotDecl.NUMBER().getText()));
                    }
                    if (annotDecl.IDENT().getText().toLowerCase().equals("inference") && annotDecl.STRING() != null) {
                        newClass.setInference(cleanString(annotDecl.STRING().getText()));
                    }
                }
            }
        }
        //opposite completion
        model.completeOppositeReferences();
        return model;
    }

    public static void main(String[] args) throws IOException {
        System.out.println(build(new File("/Users/duke/Documents/dev/kevoree-modeling/framework/org.kevoree.modeling.ast/src/main/exemples/cloud.mm")));
    }

    private MModelReference getOrAddReference(MModelClass owner, String refName, MModelClass refType) {
        for (MModelReference registeredRef : owner.getReferences()) {
            if (registeredRef.getName().equals(refName)) {
                return registeredRef;
            }
        }
        MModelReference reference = new MModelReference(refName, refType, owner.globalIndex);
        owner.globalIndex++;
        owner.addReference(reference);
        return reference;
    }

    private MModelEnum getOrAddEnum(String clazz) {
        MModelClassifier resolved = get(clazz);
        if (resolved == null) {
            String relationTypePackage = clazz.substring(0, clazz.lastIndexOf("."));
            String relationTypeName = clazz.substring(clazz.lastIndexOf(".") + 1);
            resolved = new MModelEnum(relationTypeName);
            resolved.setPack(relationTypePackage);
            addClassifier(resolved);
            return (MModelEnum) resolved;
        } else {
            if (resolved instanceof MModelEnum) {
                return (MModelEnum) resolved;
            } else {
                throw new RuntimeException("Naming conflict for " + clazz + ", cannot merge an enum and a class declaration");
            }
        }
    }

    private MModelClass getOrAddClass(String clazz) {
        MModelClassifier resolved = get(clazz);
        if (resolved == null) {
            String relationTypePackage = clazz.substring(0, clazz.lastIndexOf("."));
            String relationTypeName = clazz.substring(clazz.lastIndexOf(".") + 1);
            resolved = new MModelClass(relationTypeName);
            resolved.setPack(relationTypePackage);
            addClassifier(resolved);
            return (MModelClass) resolved;
        } else {
            if (resolved instanceof MModelClass) {
                return (MModelClass) resolved;
            } else {
                throw new RuntimeException("Naming conflict for " + clazz + ", cannot merge an enum and a class declaration");
            }
        }
    }

    private void completeOppositeReferences() {
        for (MModelClass classDecl : getClasses()) {
            for (MModelReference ref : classDecl.getReferences().toArray(new MModelReference[classDecl.getReferences().size()])) {
                if (ref.getOpposite() == null) {
                    //Create opposite relation
                    MModelReference op_ref = new MModelReference("op_" + classDecl.getName() + "_" + ref.getName(), classDecl, classDecl.globalIndex);
                    classDecl.globalIndex++;
                    op_ref.setVisible(false);
                    op_ref.setSingle(false);
                    op_ref.setOpposite(ref.getName());

                    //add the relation on  the other side
                    ref.getType().addReference(op_ref);
                    ref.setOpposite(op_ref.getName());
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (MModelClassifier cl : classifiers.values()) {
            builder.append(cl.toString());
        }
        return builder.toString();
    }
}
