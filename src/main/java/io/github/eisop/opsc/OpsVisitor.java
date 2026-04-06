package io.github.eisop.opsc;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import io.github.eisop.opsc.log.OpsLogger;
import io.github.eisop.opsc.qual.Sql;
import io.github.eisop.opsc.qual.SqlUnsupported;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        preparedStatementSetMethodTypes.addAll(typeMapping.getSetterMethods(processingEnv));
        resultSetGetByNameMethodTypes.addAll(typeMapping.getGetterByNameMethods(processingEnv));
        resultSetGetByIndexMethodTypes.addAll(typeMapping.getGetterByIndexMethods(processingEnv));
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

        if (isNonLocal(receiverType)) {
            checker.reportWarning(tree, "nonlocal.prepared.statement");
            logNonlocal(tree, "nonlocal.prepared.statement", "method=" + method.getSimpleName());
            return;
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
            checker.reportError(tree, "parameter.type.incompatible", methodName, jdbcType);
            logError(
                    tree,
                    "parameter." + result.getDetails(),
                    "expected=" + methodName + ", actual=" + jdbcType,
                    sqlAnnotation);
        } else if (result.getKind() == OpsCheckResultKind.WARNING) {
            checker.reportWarning(
                    tree, result.getDetails(), methodName, jdbcType, result.getDetails());
            logWarning(
                    tree,
                    "parameter." + result.getDetails(),
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

        if (isNonLocal(receiverType)) {
            checker.reportWarning(tree, "nonlocal.result.set");
            logNonlocal(tree, "nonlocal.result.set", "method=" + method.getSimpleName());
            return;
        }

        if (receiverType.hasAnnotation(Sql.class)) {
            AnnotationMirror sqlAnnotation = receiverType.getAnnotation(Sql.class);
            ExpressionTree indexTree = tree.getArguments().get(0);
            int literalIndex = retrieveIntValue(indexTree);
            if (literalIndex == -1) {
                checker.reportError(tree, "column.index.cannot.be.determined");
                logError(tree, "column.index.not.cannot.be.determined", "", sqlAnnotation);
            } else {
                int columnIndex = literalIndex - 1; // ResultSet columns are 1-indexed
                checkGetResult(tree, method.getSimpleName().toString(), sqlAnnotation, columnIndex);
            }
        }
    }

    private void checkGetResultByName(MethodInvocationTree tree, ExecutableElement method) {
        AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(tree);
        if (receiverType == null) {
            throw new TypeSystemError("Could not find receiver of method invocation");
        }

        if (isNonLocal(receiverType)) {
            checker.reportWarning(tree, "nonlocal.result.set");
            logNonlocal(tree, "nonlocal.result.set", "method=" + method.getSimpleName());
            return;
        }

        if (receiverType.hasAnnotation(Sql.class)) {
            AnnotationMirror sqlAnnotation = receiverType.getAnnotation(Sql.class);
            ExpressionTree indexTree = tree.getArguments().get(0);
            AnnotationMirror stringValAnno = atypeFactory.getStringValAnnoMirror(indexTree);
            List<String> stringValues =
                    OpsUtils.retrieveStringValues(
                            stringValAnno, atypeFactory.stringValValueElement);
            if (stringValues.size() != 1) {
                checker.reportWarning(indexTree, "column.name.extraction.failed");
                logWarning(tree, "column.name.extraction.failed", "", sqlAnnotation);
                return;
            }

            String columnName = stringValues.get(0);
            List<String> out =
                    AnnotationUtils.getElementValueArray(
                            sqlAnnotation,
                            sqlOutElement,
                            String.class,
                            Collections.emptyList());

            Optional<String> matchedColumn = out.stream()
                    .filter(annoColumnName -> columnNamesMatch(annoColumnName, columnName))
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
                        "expected=" + methodName + ", actual=" + out.get(index),
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
                        "expected=" + methodName + ", actual=" + out.get(index),
                        sqlAnnotation);
            } else {
                logOk(tree, "column.get", sqlAnnotation);
            }
        }
    }

    private boolean isNonLocal(AnnotatedTypeMirror type) {
        return !(type.hasAnnotation(Sql.class) || type.hasAnnotation(SqlUnsupported.class));
    }

    private boolean columnNamesMatch(String fromAnno, String fromCall) {
        String name = getName(fromAnno);
        return name != null && name.equalsIgnoreCase(fromCall);
    }

    static @Nullable String getName(String annotationString) {
        // todo improve: e.g. with class for OPSC type
        String[] tokens = annotationString.split(" ", -1);
        if (tokens.length >= 2) {
            return tokens[tokens.length - 1];
        } else {
            return null;
        }
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

    private void logNonlocal(MethodInvocationTree tree, String key, String details) {
        logger.warningRelatedToStatement(
                root,
                trees.getSourcePositions().getStartPosition(root, tree),
                "",
                "",
                "",
                key,
                details);
    }
}
