package io.github.eisop.opsc;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import io.github.eisop.opsc.db.SchemaInfo;
import io.github.eisop.opsc.exception.OpsDatabaseException;
import io.github.eisop.opsc.qual.Sql;
import io.github.eisop.opsc.qual.SqlBottom;
import io.github.eisop.opsc.qual.SqlUnknown;
import java.lang.annotation.Annotation;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
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

    private final ExecutableElement connectionPrepareStatement =
            TreeUtils.getMethod("java.sql.Connection", "prepareStatement", 1, processingEnv);

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
            try {
                schemaInfo =
                        new SchemaInfo(
                                checker.getOption("dbUrl"),
                                checker.getOption("dbUser"),
                                checker.getOption("dbPassword"));
            } catch (SQLException e) {
                throw new UserError("Could not get schema from database");
            }
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

                if (Arrays.asList(sup).contains("@NotNull")
                        && !Arrays.asList(sub).contains("@NotNull")) {
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
                                    .get();
                    int subMax =
                            Arrays.stream(sub)
                                    .filter(s -> s.startsWith("@MaxLength("))
                                    .map(
                                            s ->
                                                    Integer.parseInt(
                                                            s.split("\\(", 2)[1]
                                                                    .split("\\)", 2)[0]))
                                    .findFirst()
                                    .get();
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
                    if (arg.getKind() == ExpressionTree.Kind.STRING_LITERAL) {
                        String stmt = (String) ((LiteralTree) arg).getValue();

                        // get result type and placeholder type of prepared statement
                        String[] in = getInType(stmt);
                        String[] out = getOutType(stmt);
                        if (out == null) {
                            checker.reportWarning(
                                    tree, "could not get result type of prepared statement");
                        }
                        if (in == null) {
                            checker.reportWarning(
                                    tree, "could not get placeholder type of prepared statement");
                        }

                        type.replaceAnnotation(createSQLAnnotation(in, out));
                    }
                }
            }

            return super.visitMethodInvocation(tree, type);
        }

        /** Returns a new SQL annotation with the given output type. */
        private AnnotationMirror createSQLAnnotation(String[] in, String[] out) {
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, Sql.class);
            if (in != null) builder.setValue("in", in);
            if (out != null) builder.setValue("out", out);
            return builder.build();
        }

        private String[] getOutType(String stmt) {
            try {
                return schemaInfo.getResultTypeOf(stmt).toArray(new String[0]);
            } catch (OpsDatabaseException e) {
                return null;
            }
        }

        private String[] getInType(String stmt) {
            try {
                return schemaInfo.getPlaceholderTypesOf(stmt).toArray(new String[0]);
            } catch (OpsDatabaseException e) {
                return null;
            }
        }
    }
}
