package io.github.eisop.opsc;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import io.github.eisop.opsc.db.CalciteSchemaInfo;
import io.github.eisop.opsc.db.JDBCSchemaInfo;
import io.github.eisop.opsc.db.SchemaInfo;
import io.github.eisop.opsc.exception.OpsDatabaseException;
import io.github.eisop.opsc.log.OpsLogEntryKind;
import io.github.eisop.opsc.log.OpsLogger;
import io.github.eisop.opsc.qual.Sql;
import io.github.eisop.opsc.qual.SqlBottom;
import io.github.eisop.opsc.qual.SqlUnknown;
import io.github.eisop.opsc.qual.SqlUnsupported;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
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
import org.jspecify.annotations.Nullable;

public class OpsAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected final AnnotationMirror SQL = AnnotationBuilder.fromClass(elements, Sql.class);
    protected final AnnotationMirror SQLUNSUPPORTED =
            AnnotationBuilder.fromClass(elements, SqlUnsupported.class);
    protected final AnnotationMirror SQLUNKNOWN =
            AnnotationBuilder.fromClass(elements, SqlUnknown.class);
    protected final AnnotationMirror SQLBOTTOM =
            AnnotationBuilder.fromClass(elements, SqlBottom.class);
    protected final ExecutableElement sqlInElement =
            TreeUtils.getMethod("io.github.eisop.opsc.qual.Sql", "in", 0, processingEnv);
    protected final ExecutableElement sqlOutElement =
            TreeUtils.getMethod("io.github.eisop.opsc.qual.Sql", "out", 0, processingEnv);
    protected final ExecutableElement sqlFileElement =
            TreeUtils.getMethod("io.github.eisop.opsc.qual.Sql", "file", 0, processingEnv);
    protected final ExecutableElement sqlLineElement =
            TreeUtils.getMethod("io.github.eisop.opsc.qual.Sql", "line", 0, processingEnv);
    protected final ExecutableElement sqlColumnElement =
            TreeUtils.getMethod("io.github.eisop.opsc.qual.Sql", "column", 0, processingEnv);
    private final OpsLogger logger = ((OpsChecker) checker).getLogger();
    private final ExecutableElement stringValValueElement =
            TreeUtils.getMethod(
                    "org.checkerframework.common.value.qual.StringVal", "value", 0, processingEnv);

    private final List<ExecutableElement> connectionPrepareStatementMethods;
    private final List<ExecutableElement> statementToResultSetMethods;
    private final List<ExecutableElement> sqlUnsupportedMethods;

    private final ExecutableElement statementExecuteQuery =
            TreeUtils.getMethod("java.sql.Statement", "executeQuery", 1, processingEnv);

    private SchemaInfo calciteSchemaInfo;

    // Used as fallback
    private SchemaInfo jdbcSchemaInfo;

    @SuppressWarnings("this-escape") // Call to postInit().
    public OpsAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        // prepareStatement methods are overloaded with 1, 2, 3, or 4 parameters
        connectionPrepareStatementMethods =
                TreeUtils.getMethods("java.sql.Connection", "prepareStatement", 1, processingEnv);
        connectionPrepareStatementMethods.addAll(
                TreeUtils.getMethods("java.sql.Connection", "prepareStatement", 2, processingEnv));
        connectionPrepareStatementMethods.addAll(
                TreeUtils.getMethods("java.sql.Connection", "prepareStatement", 3, processingEnv));
        connectionPrepareStatementMethods.addAll(
                TreeUtils.getMethods("java.sql.Connection", "prepareStatement", 4, processingEnv));

        statementToResultSetMethods =
                TreeUtils.getMethods(
                        "java.sql.PreparedStatement", "executeQuery", 0, processingEnv);
        statementToResultSetMethods.addAll(
                TreeUtils.getMethods("java.sql.Statement", "getResultSet", 0, processingEnv));

        sqlUnsupportedMethods =
                TreeUtils.getMethods("java.sql.Connection", "prepareCall", 1, processingEnv);
        sqlUnsupportedMethods.addAll(
                TreeUtils.getMethods("java.sql.Connection", "prepareCall", 3, processingEnv));
        sqlUnsupportedMethods.addAll(
                TreeUtils.getMethods("java.sql.Connection", "prepareCall", 4, processingEnv));
        sqlUnsupportedMethods.addAll(
                TreeUtils.getMethods("java.sql.Statement", "getGeneratedKeys", 0, processingEnv));

        initSchemaInfo(checker);

        this.postInit();
    }

    static @Nullable String getName(String annotationString) {
        // todo improve: e.g. with class for OPSC type
        String[] tokens = annotationString.split(" ", -1);
        if (tokens.length >= 2 && !tokens[tokens.length - 2].startsWith("@")) {
            return tokens[tokens.length - 1];
        } else {
            return null;
        }
    }

    @EnsuresNonNull({"calciteSchemaInfo", "jdbcSchemaInfo"})
    private void initSchemaInfo(
            @UnderInitialization OpsAnnotatedTypeFactory this, BaseTypeChecker checker) {
        if (checker.getOption("dbUrl") == null) {
            throw new UserError("Database URL not specified");
        }
        try {
            calciteSchemaInfo =
                    new CalciteSchemaInfo(
                            checker.getOption("dbUrl"),
                            checker.getOption("dbUser"),
                            checker.getOption("dbPassword"));
            jdbcSchemaInfo =
                    new JDBCSchemaInfo(
                            checker.getOption("dbUrl"),
                            checker.getOption("dbUser"),
                            checker.getOption("dbPassword"));
        } catch (OpsDatabaseException e) {
            throw new UserError("Could not connect to database: %s", e.getMessage());
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

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(super.createTreeAnnotator(), new OpsTreeAnnotator(this));
    }

    /** Returns a new SQL annotation with the given in and out types. */
    private AnnotationMirror createSqlAnnotation(
            @Nullable List<String> in,
            @Nullable List<String> out,
            @Nullable String file,
            @Nullable String line,
            @Nullable String column) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, Sql.class);
        if (in != null) builder.setValue("in", in);
        if (out != null) builder.setValue("out", out);
        if (file != null) builder.setValue("file", file);
        if (line != null) builder.setValue("line", line);
        if (column != null) builder.setValue("column", column);
        return builder.build();
    }

    private final class OpsQualifierHierarchy extends MostlyNoElementQualifierHierarchy {
        private final QualifierKind SQL_KIND;
        private final QualifierKind SQLBOTTOM_KIND;

        // Calls to `getQualifierKind` are safe, as the `super` call already initialized everything
        // necessary.
        // TODO: Remove once https://github.com/eisop/checker-framework/issues/1143 is fixed.
        @SuppressWarnings("nullness:method.invocation.invalid")
        private OpsQualifierHierarchy(
                Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
            super(qualifierClasses, elements, OpsAnnotatedTypeFactory.this);
            SQL_KIND = getQualifierKind(SQL);
            SQLBOTTOM_KIND = getQualifierKind(SQLBOTTOM);
        }

        @Override
        protected boolean isSubtypeWithElements(
                AnnotationMirror subAnno,
                QualifierKind subKind,
                AnnotationMirror superAnno,
                QualifierKind superKind) {
            if (subKind == SQL_KIND && superKind == SQL_KIND) {
                List<String> subIn = getInElement(subAnno);
                List<String> subOut = getOutElement(subAnno);
                List<String> superIn = getInElement(superAnno);
                List<String> superOut = getOutElement(superAnno);

                return outIsSubtype(subOut, superOut) && inIsSubtype(subIn, superIn);
            }
            throw new TypeSystemError("Unexpected qualifiers: %s %s", subAnno, superAnno);
        }

        /**
         * Returns true if the subtype's out columns are a subtype of the supertype's out columns.
         * If the supertype has n out columns, the subtypes first n out columns must match,
         * considering the type annotations. For example, {@code @Sql(out = {"@NonNull Integer",
         * "String"})} is a subtype of {@code @Sql(out = {"@Nullable Integer"})}, but not of
         * {@code @Sql(out = {"String"})}.
         *
         * @param subOut the subtype's out columns
         * @param superOut the supertype's out columns
         * @return true if the subtype's out columns are a subtype of the supertype's out columns
         */
        private boolean outIsSubtype(List<String> subOut, List<String> superOut) {
            // Compare lengths: The supertype's out columns can be a prefix of the subtype's
            // columns.
            // In this case, the remaining columns of the subtype are ignored by the supertype.
            if (subOut.size() < superOut.size()) {
                return false;
            }
            if (subOut.size() > superOut.size()) {
                return outIsSubtype(subOut.subList(0, superOut.size()), superOut);
            }

            // Compare individual columns
            for (int i = 0; i < subOut.size(); i++) {
                OpscType sub = OpscType.fromAnnotationString(subOut.get(i));
                OpscType sup = OpscType.fromAnnotationString(superOut.get(i));

                // Check if the types are equal
                if (!sub.dataTypeMatches(sup)) {
                    return false;
                }

                // Check if the column names are equal (if specified in the supertype)
                if (sup.columnName() != null
                        && !sup.columnName().equalsIgnoreCase(sub.columnName())) {
                    return false;
                }

                if (sup.columnAnnotations().contains("@NonNull")
                        && !sub.columnAnnotations().contains("@NonNull")) {
                    return false;
                }

                if (sup.columnAnnotations().stream().anyMatch(s -> s.startsWith("@MaxLength("))) {
                    if (sub.columnAnnotations().stream()
                            .noneMatch(s -> s.startsWith("@MaxLength("))) {
                        return false;
                    }
                    int superMax = getMaxLengthValue(sup.columnAnnotations());
                    int subMax = getMaxLengthValue(sub.columnAnnotations());
                    if (subMax > superMax) {
                        return false;
                    }
                }
            }
            return true;
        }

        private Integer getMaxLengthValue(List<String> columnAnnotations) {
            return columnAnnotations.stream()
                    // find @MaxLength(...) annotation
                    .filter(s -> s.startsWith("@MaxLength("))
                    .map(
                            s ->
                                    Integer.parseInt(
                                            // find the value inside the parentheses
                                            // of the @MaxLength(...) annotation
                                            s.split("\\(", 2)[1].split("\\)", 2)[0]))
                    .findFirst()
                    .orElseThrow(
                            () ->
                                    new TypeSystemError(
                                            "Invalid @MaxLength annotation: %s",
                                            (Object) columnAnnotations));
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
                if (a1 == a2) {
                    return a1;
                } else if (isSubtypeWithElements(a1, qualifierKind1, a2, qualifierKind2)) {
                    return a2;
                } else if (isSubtypeWithElements(a2, qualifierKind2, a1, qualifierKind1)) {
                    return a1;
                } else {
                    // an SQL upper bound needs at least the same in columns as a1 and a2,
                    // so a1 and a2 need to have equal in columns
                    List<String> in1 = getInElement(a1);
                    List<String> in2 = getInElement(a2);
                    List<String> out1 = getOutElement(a1);
                    List<String> out2 = getOutElement(a2);

                    if (!in1.equals(in2)) {
                        return SQLUNKNOWN;
                    }

                    // the lub has the common first out columns of a1 and a2
                    // if only the column names don't match, the lub has no name for this column
                    int maxLubOutSize = Math.min(out1.size(), out2.size());
                    List<String> outLub = new ArrayList<>();
                    for (int i = 0; i < maxLubOutSize; i++) {
                        OpscType out1Type = OpscType.fromAnnotationString(out1.get(i));
                        OpscType out2Type = OpscType.fromAnnotationString(out2.get(i));
                        if (out1Type.equals(out2Type)) {
                            outLub.add(out1.get(i));
                        } else {
                            if (out1Type.equalsIgnoringName(out2Type)) {
                                outLub.add(out1.get(i));
                            } else {
                                break;
                            }
                        }
                    }

                    return createSqlAnnotation(in1, outLub, null, null, null);
                }
            } else if (qualifierKind1 == SQL_KIND && qualifierKind2 == SQLBOTTOM_KIND) {
                return a1;
            } else if (qualifierKind1 == SQLBOTTOM_KIND && qualifierKind2 == SQL_KIND) {
                return a2;
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
                if (a1 == a2) {
                    return a1;
                } else if (isSubtypeWithElements(a1, qualifierKind1, a2, qualifierKind2)) {
                    return a1;
                } else if (isSubtypeWithElements(a2, qualifierKind1, a1, qualifierKind2)) {
                    return a2;
                } else {
                    return SQLBOTTOM;
                }
            } else if ((qualifierKind1 == SQLBOTTOM_KIND && qualifierKind2 == SQL_KIND)
                    || (qualifierKind1 == SQL_KIND && qualifierKind2 == SQLBOTTOM_KIND)) {
                return SQLBOTTOM;
            }

            throw new TypeSystemError("Unexpected qualifiers: %s %s", a1, a2);
        }
    }

    private class OpsTreeAnnotator extends TreeAnnotator {
        public OpsTreeAnnotator(BaseAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /**
         * Add annotation for Strings in prepareStatement() calls and transfer annotation from
         * PreparedStatement to ResultSet in executeQuery() calls.
         */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            if (tree.getArguments().size() > 4) return super.visitMethodInvocation(tree, type);

            if (isConnectionPrepareStatementMethodInvocation(tree)) {
                // Analyse the statement and annotate the returned type
                type.replaceAnnotation(annotateStatement(tree, true));
            } else if (TreeUtils.isMethodInvocation(tree, statementExecuteQuery, processingEnv)) {
                // Analyse the statement and annotate the returned type
                type.replaceAnnotation(annotateStatement(tree, false));
            } else if (isStatementToResultSetMethodInvocation(tree)) {
                // get type annotation from PreparedStatement and transfer it to the ResultSet
                AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(tree);
                if (receiverType == null) {
                    throw new TypeSystemError("could not get receiver type of PreparedStatement");
                }

                AnnotationMirror sqlUnsupportedAnnotation =
                        receiverType.getAnnotation(SqlUnsupported.class);
                AnnotationMirror sqlAnnotation = receiverType.getAnnotation(Sql.class);

                if (sqlUnsupportedAnnotation != null) {
                    // Transfer @SqlUnsupported from PreparedStatement to ResultSet
                    type.replaceAnnotation(sqlUnsupportedAnnotation);
                } else if (sqlAnnotation != null) {
                    List<String> out =
                            AnnotationUtils.getElementValueArray(
                                    sqlAnnotation,
                                    sqlOutElement,
                                    String.class,
                                    Collections.emptyList());
                    String file =
                            AnnotationUtils.getElementValue(
                                    sqlAnnotation, sqlFileElement, String.class, null);
                    String line =
                            AnnotationUtils.getElementValue(
                                    sqlAnnotation, sqlLineElement, String.class, null);
                    String column =
                            AnnotationUtils.getElementValue(
                                    sqlAnnotation, sqlColumnElement, String.class, null);
                    type.replaceAnnotation(createSqlAnnotation(null, out, file, line, column));
                }
            } else if (isSqlUnsupportedMethodInvocation(tree)) {
                type.replaceAnnotation(SQLUNSUPPORTED);
            }
            return super.visitMethodInvocation(tree, type);
        }

        private boolean isConnectionPrepareStatementMethodInvocation(MethodInvocationTree tree) {
            return connectionPrepareStatementMethods.stream()
                    .anyMatch(m -> TreeUtils.isMethodInvocation(tree, m, processingEnv));
        }

        private boolean isStatementToResultSetMethodInvocation(MethodInvocationTree tree) {
            int argSize = tree.getArguments().size();
            if (argSize > 1) {
                return false;
            }
            return statementToResultSetMethods.stream()
                    .anyMatch(m -> TreeUtils.isMethodInvocation(tree, m, processingEnv));
        }

        private boolean isSqlUnsupportedMethodInvocation(MethodInvocationTree tree) {
            int argSize = tree.getArguments().size();
            if (argSize == 2 || argSize > 4) {
                return false;
            }
            return sqlUnsupportedMethods.stream()
                    .anyMatch(m -> TreeUtils.isMethodInvocation(tree, m, processingEnv));
        }
    }

    private List<String> getInElement(AnnotationMirror a1) {
        return AnnotationUtils.getElementValueArray(
                a1, sqlInElement, String.class, Collections.emptyList());
    }

    private List<String> getOutElement(AnnotationMirror a1) {
        return AnnotationUtils.getElementValueArray(
                a1, sqlOutElement, String.class, Collections.emptyList());
    }

    /**
     * Returns @Sql annotation for a declaration call, or @SqlUnsupported if not extractable or not
     * parsable. Success or errors are logged and appropriate warnings emitted.
     */
    protected AnnotationMirror annotateStatement(
            MethodInvocationTree tree, boolean isPreparedStatement) {
        ExpressionTree arg = tree.getArguments().get(0);
        List<String> stmts = retrieveStringValue(arg, isPreparedStatement);

        if (stmts == null) {
            // if stmts == null retrieveStringValue already reported an error
            return SQLUNSUPPORTED;
        }

        // If multiple strings are possible, determine types for all of them and if they are the
        // same for
        // all strings, continue.

        // determine types for the first string, with warnings flag activated
        List<AnnotationMirror> annos = new ArrayList<>();

        boolean first = true;
        for (String stmt : stmts) {
            AnnotationMirror anno = buildSqlAnnotation(stmt, tree, isPreparedStatement, first);
            if (anno == null) {
                return SQLUNSUPPORTED;
            }
            annos.add(anno);
            first = false;
        }

        if (annos.isEmpty()) {
            return SQLUNSUPPORTED;
        }

        // warn if the annotations are not equal
        if (annos.size() > 1
                && !annos.stream().allMatch(a -> sqlAnnotationsEqual(a, annos.get(0)))) {
            checker.reportWarning(arg, "statement.multiple.string.values", stmts.toString());
            logger.simpleStatementEntry(
                    OpsLogEntryKind.CANNOT_DETERMINE_STATEMENT_STRING,
                    getRoot(),
                    getStartPosition(arg),
                    "statement string could evaluate to multiple string values",
                    isPreparedStatement);
            return SQLUNSUPPORTED;
        }

        AnnotationMirror annotation = annos.get(0);
        String details = annos.get(0).toString();
        logSupportedStatement(
                tree, details, stmts.get(0), getInElement(annotation).size(), isPreparedStatement);
        return annotation;
    }

    /**
     * Determines result and placeholder types of the SQL statement and builds a corresponding @Sql
     * annotation.
     *
     * <p>Hack: Because Calcite doesn't support statements like 'SELECT ?', use JDBCSchemaInfo as a
     * fallback if
     *
     * @return the @Sql annotation or null if the types could not be determined
     */
    private @Nullable AnnotationMirror buildSqlAnnotation(
            String stmt,
            MethodInvocationTree tree,
            boolean isPreparedStatement,
            boolean warnAndLog) {
        // get placeholder types of prepared statement
        List<String> in;
        try {
            in = getInType(stmt, calciteSchemaInfo);
        } catch (OpsDatabaseException calciteException) {
            // Retry with fallback JDBCSchemaInfo
            try {
                in = getInType(stmt, jdbcSchemaInfo);
                if (warnAndLog) {
                    checker.reportWarning(
                            tree,
                            "determine.in.type.failed.first.try",
                            calciteException.getMessage() == null
                                    ? ""
                                    : calciteException.getMessage(),
                            stmt);
                }
            } catch (OpsDatabaseException jdbcException) {
                if (!warnAndLog) return null;
                checker.reportError(
                        tree,
                        "determine.in.type.failed.final",
                        calciteException.getMessage() + "\nJDBC: " + jdbcException.getMessage(),
                        stmt);
                logger.unsupportedPreparedStatement(
                        getRoot(),
                        getStartPosition(tree),
                        calciteException.getMessage() + "--- JDBC: " + jdbcException.getMessage(),
                        stmt,
                        isPreparedStatement);
                return null;
            }
            if (warnAndLog) {
                logger.simpleStatementEntry(
                        OpsLogEntryKind.USING_FALLBACK,
                        getRoot(),
                        getStartPosition(tree),
                        calciteException.getMessage(),
                        isPreparedStatement);
            }
        }

        // get result type of prepared statement
        List<String> out;
        try {
            out = getOutType(stmt, calciteSchemaInfo);
        } catch (OpsDatabaseException calciteException) {
            // Retry with fallback JDBCSchemaInfo
            try {
                out = getOutType(stmt, jdbcSchemaInfo);
                if (warnAndLog) {
                    checker.reportWarning(
                            tree,
                            "determine.out.type.failed.first.try",
                            calciteException.getMessage() == null
                                    ? ""
                                    : calciteException.getMessage(),
                            stmt);
                }
            } catch (OpsDatabaseException jdbcException) {
                if (!warnAndLog) return null;
                checker.reportError(
                        tree,
                        "determine.out.type.failed.final",
                        jdbcException.getMessage() == null ? "" : jdbcException.getMessage(),
                        stmt);
                logger.unsupportedPreparedStatement(
                        getRoot(),
                        getStartPosition(tree),
                        calciteException.getMessage() + "--- JDBC: " + jdbcException.getMessage(),
                        stmt,
                        isPreparedStatement);
                return null;
            }
            if (warnAndLog) {
                logger.simpleStatementEntry(
                        OpsLogEntryKind.USING_FALLBACK,
                        getRoot(),
                        getStartPosition(tree),
                        calciteException.getMessage(),
                        isPreparedStatement);
            }
        }

        String file = null;
        String line = null;
        String column = null;
        CompilationUnitTree root = getRoot();
        if (root != null) {
            file = logger.sanitizeFileName(root.getSourceFile().getName());
            LineMap lineMap = root.getLineMap();
            long loc = trees.getSourcePositions().getStartPosition(root, tree);
            line = String.valueOf(lineMap.getLineNumber(loc));
            column = String.valueOf(lineMap.getColumnNumber(loc));
        }

        return createSqlAnnotation(in, out, file, line, column);
    }

    private @Nullable List<String> getOutType(String stmt, SchemaInfo schemaInfo)
            throws OpsDatabaseException {
        List<String> rt = schemaInfo.getResultTypeOf(stmt);
        if (rt == null || rt.isEmpty()) {
            return null;
        }
        return rt;
    }

    private @Nullable List<String> getInType(String stmt, SchemaInfo schemaInfo)
            throws OpsDatabaseException {
        List<String> pt = schemaInfo.getPlaceholderTypesOf(stmt);
        if (pt == null || pt.isEmpty()) {
            return null;
        }
        return pt;
    }

    protected boolean sqlAnnotationsEqual(AnnotationMirror a1, AnnotationMirror a2) {
        // compare in and out elements
        return a1 == a2
                || (getInElement(a1).equals(getInElement(a2))
                        && getOutElement(a1).equals(getOutElement(a2)));
    }

    private long getStartPosition(Tree tree) {
        CompilationUnitTree root = getRoot();
        if (root == null) {
            return -1;
        }
        return trees.getSourcePositions().getStartPosition(root, tree);
    }

    private @Nullable List<String> retrieveStringValue(
            ExpressionTree stringExpression, boolean isPreparedStatement) {
        if (stringExpression.getKind() == ExpressionTree.Kind.STRING_LITERAL) {
            return List.of((String) ((LiteralTree) stringExpression).getValue());
        }

        AnnotationMirror stringValAnnoMirror = getStringValAnnoMirror(stringExpression);
        if (stringValAnnoMirror == null) {
            checker.reportWarning(stringExpression, "statement.string.retrieval.failed");
            logger.simpleStatementEntry(
                    OpsLogEntryKind.CANNOT_DETERMINE_STATEMENT_STRING,
                    getRoot(),
                    getStartPosition(stringExpression),
                    "",
                    isPreparedStatement);
            return null;
        }

        List<String> values =
                AnnotationUtils.getElementValueArray(
                        stringValAnnoMirror,
                        stringValValueElement,
                        String.class,
                        Collections.emptyList());

        if (values.isEmpty()) {
            checker.reportWarning(stringExpression, "statement.string.retrieval.failed");
            logger.simpleStatementEntry(
                    OpsLogEntryKind.CANNOT_DETERMINE_STATEMENT_STRING,
                    getRoot(),
                    getStartPosition(stringExpression),
                    "",
                    isPreparedStatement);
            return null;
        }

        if (values.size() == 1) {
            return values;
        }

        if (checker.getBooleanOption("enableSqlStringHeuristic")) { // todo: remove this option?
            checker.reportWarning(
                    stringExpression,
                    "statement.multiple.string.values.continuing",
                    values.toString());
            logger.simpleStatementEntry(
                    OpsLogEntryKind.USING_SQL_STRING_HEURISTIC,
                    getRoot(),
                    getStartPosition(stringExpression),
                    null,
                    isPreparedStatement);
            return values.subList(0, 1);
        }

        return values;
    }

    private @Nullable AnnotationMirror getStringValAnnoMirror(final ExpressionTree valueExp) {
        ValueAnnotatedTypeFactory valueAnnotatedTypeFactory =
                getTypeFactoryOfSubchecker(ValueChecker.class);
        if (valueAnnotatedTypeFactory == null) {
            throw new TypeSystemError("Missing subchecker ValueChecker");
        }
        AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(valueExp);
        return valueType.getAnnotation(StringVal.class);
    }

    protected void logSupportedStatement(
            MethodInvocationTree tree,
            String details,
            String stmt,
            int nParameters,
            boolean isPreparedStatement) {
        logger.supportedStatement(
                getRoot(), getStartPosition(tree), details, stmt, nParameters, isPreparedStatement);
    }
}
