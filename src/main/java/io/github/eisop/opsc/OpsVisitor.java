package io.github.eisop.opsc;

import com.google.common.base.Splitter;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import io.github.eisop.opsc.qual.Sql;
import java.util.Collections;
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

    private final Map<ExecutableElement, String> preparedStatementSetMethodTypes =
            Map.ofEntries(
                    Map.entry(
                            TreeUtils.getMethod(
                                    "java.sql.PreparedStatement", "setString", 2, processingEnv),
                            "String"),
                    Map.entry(
                            TreeUtils.getMethod(
                                    "java.sql.PreparedStatement", "setInt", 2, processingEnv),
                            "Integer"),
                    Map.entry(
                            TreeUtils.getMethod(
                                    "java.sql.PreparedStatement", "setDouble", 2, processingEnv),
                            "Double"),
                    Map.entry(
                            TreeUtils.getMethod(
                                    "java.sql.PreparedStatement", "setBoolean", 2, processingEnv),
                            "Boolean"));

    private final Map<ExecutableElement, String> resultSetGetMethodTypes =
            Map.ofEntries(
                    Map.entry(
                            TreeUtils.getMethod(
                                    "java.sql.ResultSet", "getString", processingEnv, "int"),
                            "String"),
                    Map.entry(
                            TreeUtils.getMethod(
                                    "java.sql.ResultSet", "getInt", processingEnv, "int"),
                            "Integer"),
                    Map.entry(
                            TreeUtils.getMethod(
                                    "java.sql.ResultSet", "getDouble", processingEnv, "int"),
                            "Double"),
                    Map.entry(
                            TreeUtils.getMethod(
                                    "java.sql.ResultSet", "getBoolean", processingEnv, "int"),
                            "Boolean"));

    public OpsVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public OpsAnnotatedTypeFactory createTypeFactory() {
        return new OpsAnnotatedTypeFactory(checker);
    }

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
                    checker.reportError(tree, "parameter.index.outOfBounds");
                } else if (!javaTypesMatch(
                        in.get(index), preparedStatementSetMethodTypes.get(method))) {
                    checker.reportError(tree, "parameter.type.incompatible");
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
                    checker.reportError(tree, "column.index.outOfBounds");
                } else if (!javaTypesMatch(out.get(index), resultSetGetMethodTypes.get(method))) {
                    checker.reportError(tree, "column.type.incompatible");
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
