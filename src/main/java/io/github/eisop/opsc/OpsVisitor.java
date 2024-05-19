package io.github.eisop.opsc;

import com.google.common.base.Splitter;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import io.github.eisop.opsc.qual.Sql;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;

public class OpsVisitor extends BaseTypeVisitor<OpsAnnotatedTypeFactory> {

    private final ProcessingEnvironment processingEnv = checker.getProcessingEnvironment();

    private final ExecutableElement sqlInElement =
            TreeUtils.getMethod("io.github.eisop.opsc.qual.Sql", "in", 0, processingEnv);

    private final ExecutableElement sqlOutElement =
            TreeUtils.getMethod("io.github.eisop.opsc.qual.Sql", "out", 0, processingEnv);

//    private final Map<ExecutableElement, String> preparedStatementSetMethodTypes =
//            Map.ofEntries(
//                    Map.entry(
//                            TreeUtils.getMethod(
//                                    "java.sql.PreparedStatement", "setString", 2, processingEnv),
//                            "String"),
//                    Map.entry(
//                            TreeUtils.getMethod(
//                                    "java.sql.PreparedStatement", "setInt", 2, processingEnv),
//                            "Integer"),
//                    Map.entry(
//                            TreeUtils.getMethod(
//                                    "java.sql.PreparedStatement", "setDouble", 2, processingEnv),
//                            "Double"),
//                    Map.entry(
//                            TreeUtils.getMethod(
//                                    "java.sql.PreparedStatement",
//                                    "setBigDecimal",
//                                    2,
//                                    processingEnv),
//                            "BigDecimal"),
//                    Map.entry(
//                            TreeUtils.getMethod(
//                                    "java.sql.PreparedStatement", "setBoolean", 2, processingEnv),
//                            "Boolean"));
//
    // Java 8 does not support Map.ofEntries and Map.entry
    private final Map<ExecutableElement, String> preparedStatementSetMethodTypes =
        new HashMap<ExecutableElement, String>() {{
            put(
                TreeUtils.getMethod(
                    "java.sql.PreparedStatement", "setString", 2, processingEnv),
                "String");
            put(
                TreeUtils.getMethod(
                    "java.sql.PreparedStatement", "setInt", 2, processingEnv),
                "Integer");
            put(
                TreeUtils.getMethod(
                    "java.sql.PreparedStatement", "setDouble", 2, processingEnv),
                "Double");
            put(
                TreeUtils.getMethod(
                    "java.sql.PreparedStatement",
                    "setBigDecimal",
                    2,
                    processingEnv),
                "BigDecimal");
            put(
                TreeUtils.getMethod(
                    "java.sql.PreparedStatement", "setBoolean", 2, processingEnv),
                "Boolean");
            put(
                TreeUtils.getMethod(
                 "java.sql.PreparedStatement", "setObject", 2, processingEnv),
                "Object");
        }};

//    private final Map<ExecutableElement, String> resultSetGetMethodTypes =
//            Map.ofEntries(
//                    Map.entry(
//                            TreeUtils.getMethod(
//                                    "java.sql.ResultSet", "getString", processingEnv, "int"),
//                            "String"),
//                    Map.entry(
//                            TreeUtils.getMethod(
//                                    "java.sql.ResultSet", "getInt", processingEnv, "int"),
//                            "Integer"),
//                    Map.entry(
//                            TreeUtils.getMethod(
//                                    "java.sql.ResultSet", "getDouble", processingEnv, "int"),
//                            "Double"),
//                    Map.entry(
//                            TreeUtils.getMethod(
//                                    "java.sql.ResultSet", "getBigDecimal", processingEnv, "int"),
//                            "BigDecimal"),
//                    Map.entry(
//                            TreeUtils.getMethod(
//                                    "java.sql.ResultSet", "getBoolean", processingEnv, "int"),
//                            "Boolean"));
    // Java 8 does not support Map.ofEntries and Map.entry
    private final Map<ExecutableElement, String> resultSetGetMethodTypes =
        new HashMap<ExecutableElement, String>() {{
            put(
                TreeUtils.getMethod(
                    "java.sql.ResultSet", "getString", processingEnv, "int"),
                "String");
            put(
                TreeUtils.getMethod(
                    "java.sql.ResultSet", "getInt", processingEnv, "int"),
                "Integer");
            put(
                TreeUtils.getMethod(
                    "java.sql.ResultSet", "getDouble", processingEnv, "int"),
                "Double");
            put(
                TreeUtils.getMethod(
                    "java.sql.ResultSet", "getBigDecimal", processingEnv, "int"),
                "BigDecimal");
            put(
                TreeUtils.getMethod(
                    "java.sql.ResultSet", "getBoolean", processingEnv, "int"),
                "Boolean");
            put(
                TreeUtils.getMethod(
                    "java.sql.ResultSet", "getObject", processingEnv, "int"),
                "Object");
        }};

    public OpsVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public OpsAnnotatedTypeFactory createTypeFactory() {
        return new OpsAnnotatedTypeFactory(checker);
    }

    @SuppressWarnings("VoidUsed")
    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
        for (ExecutableElement method : preparedStatementSetMethodTypes.keySet()) {
            if (TreeUtils.isMethodInvocation(tree, method, processingEnv)) {
                checkSetParameter(tree, method);
                break;
            }
        }
        for (ExecutableElement method : resultSetGetMethodTypes.keySet()) {
            if (TreeUtils.isMethodInvocation(tree, method, processingEnv)) {
                checkGetResult(tree, method);
                break;
            }
        }

        return super.visitMethodInvocation(tree, p);
    }

    private void checkSetParameter(MethodInvocationTree tree, ExecutableElement method) {
        AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(tree);
        if (receiverType == null) {
            throw new TypeSystemError("Could not find receiver of method invocation");
        }

        if (receiverType.hasAnnotation(Sql.class)) {
            AnnotationMirror sqlAnnotation = receiverType.getAnnotation(Sql.class);
            ExpressionTree indexTree = tree.getArguments().get(0);
            if (indexTree.getKind() == Tree.Kind.INT_LITERAL) {
                LiteralTree literal = (LiteralTree) indexTree;
                int index =
                        (int) literal.getValue() - 1; // PreparedStatement parameters are 1-indexed
                List<String> in =
                        AnnotationUtils.getElementValueArray(
                                sqlAnnotation, sqlInElement, String.class, Collections.emptyList());
                if (index >= in.size()) {
                    checker.reportError(
                            tree, "parameter.index.out.of.bounds", index + 1, in.size());
                } else if (!(
                        javaTypesMatch(in.get(index), "Object")
                        || javaTypesMatch(in.get(index), preparedStatementSetMethodTypes.get(method))
                )) {
                    checker.reportError(
                            tree,
                            "parameter.type.incompatible",
                            preparedStatementSetMethodTypes.get(method),
                            in.get(index));
                }
            }
        }
    }

    private void checkGetResult(MethodInvocationTree tree, ExecutableElement method) {
        AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(tree);

        if (receiverType.hasAnnotation(Sql.class)) {
            AnnotationMirror sqlAnnotation = receiverType.getAnnotation(Sql.class);
            ExpressionTree indexTree = tree.getArguments().get(0);
            if (indexTree.getKind() == Tree.Kind.INT_LITERAL) {
                LiteralTree literal = (LiteralTree) indexTree;
                int index = (int) literal.getValue() - 1; // ResultSet columns are 1-indexed
                List<String> out =
                        AnnotationUtils.getElementValueArray(
                                sqlAnnotation,
                                sqlOutElement,
                                String.class,
                                Collections.emptyList());
                if (index >= out.size()) {
                    checker.reportError(tree, "column.index.out.of.bounds", index + 1, out.size());
                } else if (!(
                        resultSetGetMethodTypes.get(method).equals("Object")
                        || javaTypesMatch(out.get(index), resultSetGetMethodTypes.get(method))
                )) {
                    checker.reportError(
                            tree,
                            "column.type.incompatible",
                            resultSetGetMethodTypes.get(method),
                            out.get(index));
                }
            }
        }
    }

    private boolean javaTypesMatch(String type, String other) {
        List<String> split = Splitter.on(' ').splitToList(type);
        List<String> otherSplit = Splitter.on(' ').splitToList(other);
        return split.get(split.size() - 1).equals(otherSplit.get(otherSplit.size() - 1));
    }
}
