package io.github.eisop.opsc;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import io.github.eisop.opsc.log.OpsLogger;
import io.github.eisop.opsc.qual.Sql;
import java.util.*;
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

    private final OpsLogger logger = ((OpsChecker) checker).getLogger();
    private final TypeMapping typeMapping = ((OpsChecker) checker).getTypeMapping();

    private final ProcessingEnvironment processingEnv = checker.getProcessingEnvironment();
    protected final ExecutableElement sqlFileElement =
            TreeUtils.getMethod("io.github.eisop.opsc.qual.Sql", "file", 0, processingEnv);
    protected final ExecutableElement sqlLineElement =
            TreeUtils.getMethod("io.github.eisop.opsc.qual.Sql", "line", 0, processingEnv);
    protected final ExecutableElement sqlColumnElement =
            TreeUtils.getMethod("io.github.eisop.opsc.qual.Sql", "column", 0, processingEnv);
    private final ExecutableElement sqlInElement =
            TreeUtils.getMethod("io.github.eisop.opsc.qual.Sql", "in", 0, processingEnv);
    private final ExecutableElement sqlOutElement =
            TreeUtils.getMethod("io.github.eisop.opsc.qual.Sql", "out", 0, processingEnv);
    private final Set<ExecutableElement> preparedStatementSetMethodTypes = new HashSet<>();
    private final Set<ExecutableElement> resultSetGetByIndexMethodTypes = new HashSet<>();
    private final Set<ExecutableElement> resultSetGetByNameMethodTypes = new HashSet<>();

    public OpsVisitor(BaseTypeChecker checker) {
        super(checker);
        for (String s : typeMapping.getSetMethodNames()) {
            preparedStatementSetMethodTypes.addAll(
                    TreeUtils.getMethods("java.sql.PreparedStatement", s, 2, processingEnv));
        }
        typeMapping
                .getGetMethodNames()
                .forEach(
                        name -> {
                            resultSetGetByIndexMethodTypes.add(
                                    TreeUtils.getMethod(
                                            "java.sql.ResultSet", name, processingEnv, "int"));
                            resultSetGetByNameMethodTypes.add(
                                    TreeUtils.getMethod(
                                            "java.sql.ResultSet",
                                            name,
                                            processingEnv,
                                            "java.lang.String"));
                        });
    }

    @Override
    public OpsAnnotatedTypeFactory createTypeFactory() {
        return new OpsAnnotatedTypeFactory(checker);
    }

    @SuppressWarnings("VoidUsed")
    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
        int argSize = tree.getArguments().size();
        if (argSize != 1 && argSize != 2) {
            // Early exit if the method call can't be relevant.
            return super.visitMethodInvocation(tree, p);
        }

        if (argSize == 2) {
            for (ExecutableElement method : preparedStatementSetMethodTypes) {
                if (TreeUtils.isMethodInvocation(tree, method, processingEnv)) {
                    checkSetInvocation(tree, method);
                    break;
                }
            }
        } else {
            // argSize == 1;
            boolean found = false;

            for (ExecutableElement method : resultSetGetByIndexMethodTypes) {
                if (TreeUtils.isMethodInvocation(tree, method, processingEnv)) {
                    checkGetResultByIndex(tree, method);
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (ExecutableElement method : resultSetGetByNameMethodTypes) {
                    if (TreeUtils.isMethodInvocation(tree, method, processingEnv)) {
                        checkGetResultByName(tree, method);
                        // found = true; Not needed until something depends on it.
                        break;
                    }
                }
            }
        }

        return super.visitMethodInvocation(tree, p);
    }

    private void checkSetInvocation(MethodInvocationTree tree, ExecutableElement method) {
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
                    logError(
                            tree,
                            "parameter.index.out.of.bounds",
                            "index=" + index + ", size=" + in.size(),
                            sqlAnnotation);
                } else {
                    checkParameterType(
                            tree, method.getSimpleName().toString(), in.get(index), sqlAnnotation);
                }
            }
        } else {
            checker.reportWarning(tree, "parameter.preparedStatement.untyped");
            logUntyped(tree, "parameter.preparedStatement.untyped", "method name: " + method.getSimpleName());
        }
    }

    private void checkParameterType(
            MethodInvocationTree tree,
            String methodName,
            String jdbcType,
            AnnotationMirror sqlAnnotation) {
        OpsCheckResult result = typeMapping.checkCall(methodName, jdbcType);
        if (result.getKind() == OpsCheckResultKind.ERROR) {
            checker.reportError(tree, "parameter.type.incompatible", methodName, jdbcType);
            logError(
                    tree,
                    "parameter." + result.getDetails(),
                    //                    "expected=" + methodName + ", actual=" + jdbcType,
                    "SQL type=" + jdbcType + ", method=" + methodName,
                    sqlAnnotation);
        } else if (result.getKind() == OpsCheckResultKind.WARNING) {
            checker.reportWarning(
                    tree, result.getDetails(), methodName, jdbcType, result.getDetails());
            logWarning(
                    tree,
                    "parameter." + result.getDetails(),
                    "SQL type=" + jdbcType + ", method=" + methodName,
                    sqlAnnotation);
        } else {
            logOk(
                    tree,
                    "parameter.set",
                    "SQL type=" + jdbcType + ", method=" + methodName,
                    sqlAnnotation);
        }
    }

    private void checkGetResultByIndex(MethodInvocationTree tree, ExecutableElement method) {
        AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(tree);
        if (receiverType == null) {
            throw new TypeSystemError("Could not find receiver of method invocation");
        }

        if (receiverType.hasAnnotation(Sql.class)) {
            AnnotationMirror sqlAnnotation = receiverType.getAnnotation(Sql.class);
            ExpressionTree indexTree = tree.getArguments().get(0);
            if (indexTree.getKind() == Tree.Kind.INT_LITERAL) {
                LiteralTree literal = (LiteralTree) indexTree;
                int index = (int) literal.getValue() - 1; // ResultSet columns are 1-indexed
                checkGetResult(tree, method.getSimpleName().toString(), sqlAnnotation, index);
            }
        } else {
            checker.reportWarning(tree, "getter.resultSet.untyped");
            logUntyped(tree, "getter.resultSet.untyped", "method name: " + method.getSimpleName());
        }
    }

    private void checkGetResultByName(MethodInvocationTree tree, ExecutableElement method) {
        AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(tree);
        if (receiverType == null) {
            throw new TypeSystemError("Could not find receiver of method invocation");
        }

        if (receiverType.hasAnnotation(Sql.class)) {
            AnnotationMirror sqlAnnotation = receiverType.getAnnotation(Sql.class);
            ExpressionTree indexTree = tree.getArguments().get(0);
            if (indexTree.getKind() == Tree.Kind.STRING_LITERAL) {
                LiteralTree literal = (LiteralTree) indexTree;
                String columnName = (String) literal.getValue();
                List<String> out =
                        AnnotationUtils.getElementValueArray(
                                sqlAnnotation,
                                sqlOutElement,
                                String.class,
                                Collections.emptyList());
                out.stream()
                        .filter(s -> columnNamesMatch(s, columnName))
                        .findFirst()
                        .ifPresentOrElse(
                                s -> {
                                    int index = out.indexOf(s);
                                    checkGetResult(
                                            tree,
                                            method.getSimpleName().toString(),
                                            sqlAnnotation,
                                            index);
                                },
                                () -> {
                                    checker.reportError(tree, "column.name.not.found", columnName);
                                    logError(
                                            tree,
                                            "column.name.not.found",
                                            "name=" + columnName,
                                            sqlAnnotation);
                                });
            }
        } else {
            checker.reportWarning(tree, "getter.resultSet.untyped");
            logUntyped(tree, "getter.resultSet.untyped", "method name: " + method.getSimpleName());
        }
    }

    private void checkGetResult(
            MethodInvocationTree tree,
            String methodName,
            AnnotationMirror sqlAnnotation,
            int index) {
        List<String> out =
                AnnotationUtils.getElementValueArray(
                        sqlAnnotation, sqlOutElement, String.class, Collections.emptyList());
        if (index >= out.size()) {
            checker.reportError(tree, "column.index.out.of.bounds", index + 1, out.size());
            logError(
                    tree,
                    "column.index.out.of.bounds",
                    "index=" + index + ", size=" + out.size(),
                    sqlAnnotation);
        } else {
            OpsCheckResult result = typeMapping.checkCall(methodName, out.get(index));
            if (result.getKind() == OpsCheckResultKind.ERROR) {
                checker.reportError(tree, "column.type.incompatible", methodName, out.get(index));
                logError(
                        tree,
                        "column." + result.getDetails(),
                        //                        "expected=" + methodName + ", actual=" +
                        // out.get(index),
                        "SQL type=" + out.get(index) + ", method=" + methodName,
                        sqlAnnotation);
            } else if (result.getKind() == OpsCheckResultKind.WARNING) {
                checker.reportWarning(
                        tree,
                        "warning.column.types",
                        methodName,
                        out.get(index),
                        result.getDetails());
                logWarning(
                        tree,
                        "column." + result.getDetails(),
                        //                        "expected=" + methodName + ", actual=" +
                        // out.get(index),
                        "SQL type=" + out.get(index) + ", method=" + methodName,
                        sqlAnnotation);
            } else {
                logOk(
                        tree,
                        "column.get",
                        "SQL type=" + out.get(index) + ", method=" + methodName,
                        sqlAnnotation);
            }
        }
    }

    private boolean columnNamesMatch(String ann, String other) {
        String name = OpsAnnotatedTypeFactory.getName(ann);
        return name != null && name.equalsIgnoreCase(other);
    }

    private void logError(
            MethodInvocationTree tree, String key, String message, AnnotationMirror sql) {
        logger.errorRelatedToStatement(
                root,
                trees.getSourcePositions().getStartPosition(root, tree),
                AnnotationUtils.getElementValue(sql, sqlFileElement, String.class, ""),
                AnnotationUtils.getElementValue(sql, sqlLineElement, String.class, ""),
                AnnotationUtils.getElementValue(sql, sqlColumnElement, String.class, ""),
                key,
                message);
    }

    private void logWarning(
            MethodInvocationTree tree, String key, String message, AnnotationMirror sql) {
        logger.warningRelatedToStatement(
                root,
                trees.getSourcePositions().getStartPosition(root, tree),
                AnnotationUtils.getElementValue(sql, sqlFileElement, String.class, ""),
                AnnotationUtils.getElementValue(sql, sqlLineElement, String.class, ""),
                AnnotationUtils.getElementValue(sql, sqlColumnElement, String.class, ""),
                key,
                message);
    }

    private void logOk(
            MethodInvocationTree tree, String key, String message, AnnotationMirror sql) {
        logger.ok(
                root,
                trees.getSourcePositions().getStartPosition(root, tree),
                AnnotationUtils.getElementValue(sql, sqlFileElement, String.class, ""),
                AnnotationUtils.getElementValue(sql, sqlLineElement, String.class, ""),
                AnnotationUtils.getElementValue(sql, sqlColumnElement, String.class, ""),
                key,
                message);
    }

    private void logUntyped(MethodInvocationTree tree, String key, String details) {
        logger.errorRelatedToStatement(
                root,
                trees.getSourcePositions().getStartPosition(root, tree),
                "",
                "",
                "",
                key,
                details);
    }
}
