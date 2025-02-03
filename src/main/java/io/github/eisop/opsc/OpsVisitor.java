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

    private final ExecutableElement intValValueElement =
            TreeUtils.getMethod(
                    "org.checkerframework.common.value.qual.IntVal", "value", 0, processingEnv);

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
                    "java.sql.PreparedStatement", "setLong", 2, processingEnv),
                "Long");
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

    private final Map<ExecutableElement, String> resultSetGetByIndexMethodTypes =
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
                    "java.sql.ResultSet", "getLong", processingEnv, "int"),
                "Long");
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

    private final Map<ExecutableElement, String> resultSetGetByNameMethodTypes =
        new HashMap<ExecutableElement, String>() {{
            put(
                TreeUtils.getMethod(
                        "java.sql.ResultSet",
                        "getString",
                        processingEnv,
                        "java.lang.String"),
                "String");
            put(
                TreeUtils.getMethod(
                        "java.sql.ResultSet",
                        "getInt",
                        processingEnv,
                        "java.lang.String"),
                "Integer");
            put(
                TreeUtils.getMethod(
                        "java.sql.ResultSet",
                        "getLong",
                        processingEnv,
                        "java.lang.String"),
                "Long");
            put(
                TreeUtils.getMethod(
                        "java.sql.ResultSet",
                        "getDouble",
                        processingEnv,
                        "java.lang.String"),
                "Double");
            put(
                TreeUtils.getMethod(
                        "java.sql.ResultSet",
                        "getBigDecimal",
                        processingEnv,
                        "java.lang.String"),
                "BigDecimal");
            put(
                TreeUtils.getMethod(
                        "java.sql.ResultSet",
                        "getBoolean",
                        processingEnv,
                        "java.lang.String"),
                "Boolean");
            put(
                TreeUtils.getMethod(
                        "java.sql.ResultSet",
                        "getObject",
                        processingEnv,
                        "java.lang.String"),
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
        int argSize = tree.getArguments().size();
        if (argSize != 1 && argSize != 2) {
            // Early exit if the method call can't be relevant.
            return super.visitMethodInvocation(tree, p);
        }

        if (argSize == 2) {
            for (ExecutableElement method : preparedStatementSetMethodTypes.keySet()) {
                if (TreeUtils.isMethodInvocation(tree, method, processingEnv)) {
                    checkSetParameter(tree, method);
                    break;
                }
            }
        } else {
            // argSize == 1;
            boolean found = false;

            for (ExecutableElement method : resultSetGetByIndexMethodTypes.keySet()) {
                if (TreeUtils.isMethodInvocation(tree, method, processingEnv)) {
                    checkGetResultByIndex(tree, method);
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (ExecutableElement method : resultSetGetByNameMethodTypes.keySet()) {
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

    private void checkSetParameter(MethodInvocationTree tree, ExecutableElement method) {
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
                } else if (!(
                        javaTypesMatch(in.get(psIndex), "Object")
                        || javaTypesMatch(in.get(psIndex), preparedStatementSetMethodTypes.get(method))
                )) {
                    checker.reportError(
                            tree,
                            "parameter.type.incompatible",
                            preparedStatementSetMethodTypes.get(method),
                            in.get(psIndex));
                    logError(
                            tree,
                            "parameter.type.incompatible",
                            "expected="
                                    + preparedStatementSetMethodTypes.get(method)
                                    + ", actual="
                                    + in.get(psIndex),
                            sqlAnnotation);
                } else {
                    logOk(tree, "parameter.set", sqlAnnotation);
                }
            }
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
                checkGetResult(
                        tree, resultSetGetByIndexMethodTypes.get(method), sqlAnnotation, index);
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
                            resultSetGetByNameMethodTypes.get(method),
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
            String methodType,
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
        } else if (!(
                methodType.equals("Object")
                || javaTypesMatch(out.get(index), methodType)
        )) {
            checker.reportError(
                    tree,
                    "column.type.incompatible",
                    methodType,
                    out.get(index));
            logError(
                    tree,
                    "column.type.incompatible",
                    "expected=" + methodType + ", actual=" + out.get(index),
                    sqlAnnotation);
        } else {
            logOk(tree, "column.get", sqlAnnotation);
        }
    }

    private boolean javaTypesMatch(String type, String other) {
        return OpsAnnotatedTypeFactory.getType(type).equals(OpsAnnotatedTypeFactory.getType(other));
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
