package io.github.eisop.opsc;

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

    private final Map<ExecutableElement, String> methodTypes =
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
                            "Boolean"),
                    Map.entry(
                            TreeUtils.getMethod(
                                    "java.sql.PreparedStatement", "setByte", 2, processingEnv),
                            "Byte"));

    public OpsVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public OpsAnnotatedTypeFactory createTypeFactory() {
        return new OpsAnnotatedTypeFactory(checker);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
        ProcessingEnvironment processingEnv = checker.getProcessingEnvironment();
        for (ExecutableElement method : methodTypes.keySet()) {
            if (TreeUtils.isMethodInvocation(tree, method, processingEnv)) {
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
                                (int) literal.getValue()
                                        - 1; // PreparedStatement parameters are 1-indexed
                        List<String> in =
                                AnnotationUtils.getElementValueArray(
                                        sqlAnnotation,
                                        sqlInElement,
                                        String.class,
                                        Collections.emptyList());
                        if (index >= in.size()) {
                            checker.reportError(tree, "parameter.index.outOfBounds");
                        } else if (!in.get(index).equals(methodTypes.get(method))) {
                            checker.reportError(tree, "parameter.type.incompatible");
                        }
                        break;
                    }
                }
            }
        }

        return super.visitMethodInvocation(tree, p);
    }
}
