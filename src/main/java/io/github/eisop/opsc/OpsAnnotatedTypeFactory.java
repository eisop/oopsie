package io.github.eisop.opsc;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import io.github.eisop.opsc.db.JDBCSchemaInfo;
import io.github.eisop.opsc.db.SchemaInfo;
import io.github.eisop.opsc.exception.OpsDatabaseException;
import io.github.eisop.opsc.qual.Sql;
import io.github.eisop.opsc.qual.SqlBottom;
import io.github.eisop.opsc.qual.SqlUnknown;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.UserError;

public class OpsAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected final AnnotationMirror SQL = AnnotationBuilder.fromClass(elements, Sql.class);

    protected final AnnotationMirror SQLUNKNOWN =
            AnnotationBuilder.fromClass(elements, SqlUnknown.class);
    protected final AnnotationMirror SQLBOTTOM =
            AnnotationBuilder.fromClass(elements, SqlBottom.class);

    protected final ExecutableElement sqlInElement =
            TreeUtils.getMethod("io.github.eisop.opsc.qual.Sql", "in", 0, processingEnv);

    protected final ExecutableElement sqlOutElement =
            TreeUtils.getMethod("io.github.eisop.opsc.qual.Sql", "out", 0, processingEnv);

    private final ExecutableElement stringValValueELement =
            TreeUtils.getMethod(
                    "org.checkerframework.common.value.qual.StringVal", "value", 0, processingEnv);

    private final ExecutableElement connectionPrepareStatement =
            TreeUtils.getMethod("java.sql.Connection", "prepareStatement", 1, processingEnv);

    private final ExecutableElement executeQuery =
            TreeUtils.getMethod("java.sql.PreparedStatement", "executeQuery", 0, processingEnv);

    private SchemaInfo schemaInfo;

    public OpsAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        initSchemaInfo(checker);

        this.postInit();
    }

    private void initSchemaInfo(BaseTypeChecker checker) {
        if (checker.getOption("dbUrl") == null) {
            throw new UserError("Database URL not specified");
        } else {
            schemaInfo =
                    new JDBCSchemaInfo(
                            checker.getOption("dbUrl"),
                            checker.getOption("dbUser"),
                            checker.getOption("dbPassword"));
        }
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiers(SqlUnknown.class, Sql.class, SqlBottom.class);
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        return new OpsTransfer((CFAnalysis) analysis);
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new OpsQualifierHierarchy(this.getSupportedTypeQualifiers(), elements);
    }

    private final class OpsQualifierHierarchy extends MostlyNoElementQualifierHierarchy {
        private final QualifierKind SQL_KIND;

        private OpsQualifierHierarchy(
                Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
            super(qualifierClasses, elements);
            SQL_KIND = getQualifierKind(SQL);
        }

        @Override
        protected boolean isSubtypeWithElements(
                AnnotationMirror subAnno,
                QualifierKind subKind,
                AnnotationMirror superAnno,
                QualifierKind superKind) {
            if (subKind == SQL_KIND && superKind == SQL_KIND) {
                List<String> subIn =
                        AnnotationUtils.getElementValueArray(
                                subAnno, sqlInElement, String.class, Collections.emptyList());
                List<String> subOut =
                        AnnotationUtils.getElementValueArray(
                                subAnno, sqlOutElement, String.class, Collections.emptyList());
                List<String> superIn =
                        AnnotationUtils.getElementValueArray(
                                superAnno, sqlInElement, String.class, Collections.emptyList());
                List<String> superOut =
                        AnnotationUtils.getElementValueArray(
                                superAnno, sqlOutElement, String.class, Collections.emptyList());

                return outIsSubtype(subOut, superOut) && inIsSubtype(subIn, superIn);
            }
            throw new TypeSystemError("Unexpected qualifiers: %s %s", subAnno, superAnno);
        }

        private boolean outIsSubtype(List<String> subOut, List<String> superOut) {
            // Compare lengths: Supertype can have more columns than subtype as it does not have to
            // deal with all columns of the subtype
            if (subOut.size() < superOut.size()) {
                return false;
            }
            if (subOut.size() > superOut.size()) {
                return outIsSubtype(subOut, subOut.subList(0, superOut.size()));
            }

            for (int i = 0; i < subOut.size(); i++) {
                String[] sub = subOut.get(i).split(" ");
                String[] sup = superOut.get(i).split(" ");
                String subType = sub[sub.length - 1];
                String supType = sup[sup.length - 1];

                if (!subType.equals(supType)) {
                    return false;
                }

                if (Arrays.asList(sup).contains("@NonNull")
                        && !Arrays.asList(sub).contains("@NonNull")) {
                    return false;
                }

                if (Arrays.stream(sup).anyMatch(s -> s.startsWith("@MaxLength("))) {
                    if (Arrays.stream(sub).noneMatch(s -> s.startsWith("@MaxLength("))) {
                        return false;
                    }
                    int superMax =
                            Arrays.stream(sup)
                                    .filter(s -> s.startsWith("@MaxLength("))
                                    .map(
                                            s ->
                                                    Integer.parseInt(
                                                            s.split("\\(", 2)[1]
                                                                    .split("\\)", 2)[0]))
                                    .findFirst()
                                    .orElseThrow(
                                            () ->
                                                    new TypeSystemError(
                                                            "Invalid @MaxLength annotation"));
                    int subMax =
                            Arrays.stream(sub)
                                    .filter(s -> s.startsWith("@MaxLength("))
                                    .map(
                                            s ->
                                                    Integer.parseInt(
                                                            s.split("\\(", 2)[1]
                                                                    .split("\\)", 2)[0]))
                                    .findFirst()
                                    .orElseThrow(
                                            () ->
                                                    new TypeSystemError(
                                                            "Invalid @MaxLength annotation"));
                    if (subMax > superMax) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean inIsSubtype(List<String> subIn, List<String> superIn) {
            // Reverse of the hierarchy for out except that the lengths must be equal as parameters
            // have to be specified and no more parameters than exist can be specified.
            return subIn.size() == superIn.size() && outIsSubtype(superIn, subIn);
        }

        @Override
        protected AnnotationMirror leastUpperBoundWithElements(
                AnnotationMirror a1,
                QualifierKind qualifierKind1,
                AnnotationMirror a2,
                QualifierKind qualifierKind2,
                QualifierKind lubKind) {
            if (qualifierKind1 == SQL_KIND && qualifierKind2 == SQL_KIND) {
                // TODO: actually look at values.
                if (a1 == a2) {
                    return a1;
                } else {
                    return SQLUNKNOWN;
                }
            }
            throw new TypeSystemError("Unexpected qualifiers: %s %s", a1, a2);
        }

        @Override
        protected AnnotationMirror greatestLowerBoundWithElements(
                AnnotationMirror a1,
                QualifierKind qualifierKind1,
                AnnotationMirror a2,
                QualifierKind qualifierKind2,
                QualifierKind glbKind) {
            if (qualifierKind1 == SQL_KIND && qualifierKind2 == SQL_KIND) {
                // TODO: actually look at values.
                if (a1 == a2) {
                    return a1;
                } else {
                    return SQLBOTTOM;
                }
            }
            throw new TypeSystemError("Unexpected qualifiers: %s %s", a1, a2);
        }
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(super.createTreeAnnotator(), new OpsTreeAnnotator(this));
    }

    private class OpsTreeAnnotator extends TreeAnnotator {
        public OpsTreeAnnotator(BaseAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /** Add annotation for Strings in prepareStatement() calls. */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            // todo other overloaded methods
            if (TreeUtils.isMethodInvocation(tree, connectionPrepareStatement, processingEnv)) {
                ExpressionTree arg = tree.getArguments().get(0);
                if (!type.hasAnnotationRelaxed(SQL)) {
                    String stmt = retrieveStringValue(arg);
                    if (stmt != null) {
                        // get result type and placeholder type of prepared statement
                        List<String> in;
                        try {
                            in = getInType(stmt);
                        } catch (OpsDatabaseException e) {
                            throw new TypeSystemError(
                                    "Could not retrieve in type of prepared statement: %s",
                                    e.getMessage());
                        }
                        List<String> out;
                        try {
                            out = getOutType(stmt);
                        } catch (OpsDatabaseException e) {
                            throw new TypeSystemError(
                                    "Could not retrieve out type of prepared statement: %s",
                                    e.getMessage());
                        }

                        type.replaceAnnotation(createSQLAnnotation(in, out));
                    } else {
                        checker.reportWarning(
                                tree, "could not determine SQL string value of prepared statement");
                    }
                }
            } else if (TreeUtils.isMethodInvocation(tree, executeQuery, processingEnv)) {
                // get type annotation from PreparedStatement
                AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(tree);
                if (receiverType.hasAnnotation(Sql.class)) {
                    AnnotationMirror sqlAnnotation = receiverType.getAnnotation(Sql.class);
                    List<String> out =
                            AnnotationUtils.getElementValueArray(
                                    sqlAnnotation,
                                    sqlOutElement,
                                    String.class,
                                    Collections.emptyList());
                    type.replaceAnnotation(createSQLAnnotation(null, out));
                } else {
                    checker.reportWarning(
                            tree, "could not get result type annotation from PreparedStatement");
                }
            }
            return super.visitMethodInvocation(tree, type);
        }

        private @Nullable String retrieveStringValue(ExpressionTree stringExpression) {
            if (stringExpression.getKind() == ExpressionTree.Kind.STRING_LITERAL) {
                return (String) ((LiteralTree) stringExpression).getValue();
            }

            AnnotationMirror stringValAnnoMirror = getStringValAnnoMirror(stringExpression);
            if (stringValAnnoMirror != null) {
                List<String> values =
                        AnnotationUtils.getElementValueArray(
                                stringValAnnoMirror,
                                stringValValueELement,
                                String.class,
                                Collections.emptyList());
                if (values.size() == 1) {
                    return values.get(0);
                } else if (values.size() > 1) {
                    checker.reportWarning(
                            stringExpression,
                            "statement.multiple.string.values",
                            values.toString());
                    return values.get(0);
                } else {
                    return null;
                }
            }

            return null;
        }

        private AnnotationMirror getStringValAnnoMirror(final ExpressionTree valueExp) {
            ValueAnnotatedTypeFactory valueAnnotatedTypeFactory =
                    getTypeFactoryOfSubchecker(ValueChecker.class);
            if (valueAnnotatedTypeFactory == null) {
                throw new TypeSystemError("Missing subchecker ValueChecker");
            }
            AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(valueExp);
            return valueType.getAnnotation(StringVal.class);
        }

        /** Returns a new SQL annotation with the given output type. */
        private AnnotationMirror createSQLAnnotation(
                @Nullable List<String> in, @Nullable List<String> out) {
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, Sql.class);
            if (in != null) builder.setValue("in", in);
            if (out != null) builder.setValue("out", out);
            return builder.build();
        }

        private @Nullable List<String> getOutType(String stmt) throws OpsDatabaseException {
            return schemaInfo.getResultTypeOf(stmt);
        }

        private @Nullable List<String> getInType(String stmt) throws OpsDatabaseException {
            return schemaInfo.getPlaceholderTypesOf(stmt);
        }
    }
}
