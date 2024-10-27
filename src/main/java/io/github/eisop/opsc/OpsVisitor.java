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
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.qual.IntVal;
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

    private final ExecutableElement intValValueElement =
            TreeUtils.getMethod(
                    "org.checkerframework.common.value.qual.IntVal", "value", 0, processingEnv);

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
        for (ExecutableElement method : preparedStatementSetMethodTypes) {
            if (TreeUtils.isMethodInvocation(tree, method, processingEnv)) {
                checkSetInvocation(tree, method);
                break;
            }
        }
        for (ExecutableElement method : resultSetGetByIndexMethodTypes) {
            if (TreeUtils.isMethodInvocation(tree, method, processingEnv)) {
                checkGetResultByIndex(tree, method);
                break;
            }
        }
        for (ExecutableElement method : resultSetGetByNameMethodTypes) {
            if (TreeUtils.isMethodInvocation(tree, method, processingEnv)) {
                checkGetResultByName(tree, method);
                break;
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
            int literalIndex = retrieveIntValue(indexTree);
            if (literalIndex == -1) {
                checker.reportError(tree, "parameter.index.cannot.be.determined");
                logError(tree, "parameter.index.not.literal", "", sqlAnnotation);
            } else {
                int psIndex = literalIndex - 1; // PreparedStatement parameters are 1-indexed
                List<String> in =
                        AnnotationUtils.getElementValueArray(
                                sqlAnnotation, sqlInElement, String.class, Collections.emptyList());
                if (psIndex >= in.size()) {
                    checker.reportError(
                            tree, "parameter.index.out.of.bounds", psIndex + 1, in.size());
                    logError(
                            tree,
                            "parameter.index.out.of.bounds",
                            "index=" + psIndex + ", size=" + in.size(),
                            sqlAnnotation);
                } else {
                    checkParameterType(
                            tree, method.getSimpleName().toString(), in.get(psIndex), sqlAnnotation);
                }
            }
        }
    }

    private void checkParameterType(
            MethodInvocationTree tree,
            String methodName,
            String jdbcType,
            AnnotationMirror sqlAnnotation) {
        OpsCheckResult result = typeMapping.checkCall(methodName, jdbcType);
        if (result.getKind() == OpsCheckResultKind.ERROR) {
            checker.reportError(tree, result.getKey(), methodName, jdbcType);
            logError(
                    tree,
                    result.getKey(),
                    "expected=" + methodName + ", actual=" + jdbcType,
                    sqlAnnotation);
        } else if (result.getKind() == OpsCheckResultKind.WARNING) {
            checker.reportWarning(tree, result.getKey(), methodName, jdbcType);
            logWarning(
                    tree,
                    result.getKey(),
                    "expected=" + methodName + ", actual=" + jdbcType,
                    sqlAnnotation);
        } else {
            logOk(tree, "parameter.set", sqlAnnotation);
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

                Optional<String> matchedColumn = out.stream()
                        .filter(s -> columnNamesMatch(s, columnName))
                        .findFirst();
                if (matchedColumn.isPresent()) {
                    int index = out.indexOf(matchedColumn.get());
                    checkGetResult(
                            tree,
                            method.getSimpleName().toString(),
                            sqlAnnotation,
                            index);
                } else {
                    checker.reportError(tree, "column.name.not.found", columnName);
                    logError(
                            tree,
                            "column.name.not.found",
                            "name=" + columnName,
                            sqlAnnotation);
                }
            }
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
                checker.reportError(tree, result.getKey(), methodName, out.get(index));
                logError(
                        tree,
                        result.getKey(),
                        "expected=" + methodName + ", actual=" + out.get(index),
                        sqlAnnotation);
            } else if (result.getKind() == OpsCheckResultKind.WARNING) {
                checker.reportWarning(tree, result.getKey(), methodName, out.get(index));
                logWarning(
                        tree,
                        result.getKey(),
                        "expected=" + methodName + ", actual=" + out.get(index),
                        sqlAnnotation);
            } else {
                logOk(tree, "column.get", sqlAnnotation);
            }
        }
    }

    private boolean columnNamesMatch(String ann, String other) {
        String name = OpsAnnotatedTypeFactory.getName(ann);
        return name != null && name.equalsIgnoreCase(other);
    }

    private int retrieveIntValue(ExpressionTree intExpression) {
        if (intExpression.getKind() == ExpressionTree.Kind.INT_LITERAL) {
            return (int) ((LiteralTree) intExpression).getValue();
        }

        AnnotationMirror intValAnnoMirror = getIntValAnnoMirror(intExpression);
        if (intValAnnoMirror == null) {
            return -1;
        }

        List<Long> values =
                AnnotationUtils.getElementValueArray(
                        intValAnnoMirror,
                        intValValueElement,
                        Long.class,
                        Collections.emptyList());

        if (values.size() != 1) {
            return -1;
        }

        return values.get(0).intValue();
    }

    private AnnotationMirror getIntValAnnoMirror(final ExpressionTree valueExp) {
        ValueAnnotatedTypeFactory valueAnnotatedTypeFactory =
                getTypeFactory().getTypeFactoryOfSubchecker(ValueChecker.class);
        if (valueAnnotatedTypeFactory == null) {
            throw new TypeSystemError("Missing subchecker ValueChecker");
        }
        AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(valueExp);
        return valueType.getAnnotation(IntVal.class);
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

    private void logOk(MethodInvocationTree tree, String key, AnnotationMirror sql) {
        logger.ok(
                root,
                trees.getSourcePositions().getStartPosition(root, tree),
                AnnotationUtils.getElementValue(sql, sqlFileElement, String.class, ""),
                AnnotationUtils.getElementValue(sql, sqlLineElement, String.class, ""),
                AnnotationUtils.getElementValue(sql, sqlColumnElement, String.class, ""),
                key);
    }
}
