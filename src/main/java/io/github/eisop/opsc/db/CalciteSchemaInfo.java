package io.github.eisop.opsc.db;

import static io.github.eisop.opsc.db.JDBCUtil.jdbcTypeNameFromOrdinal;

import com.google.common.collect.ImmutableList;
import io.github.eisop.opsc.exception.OpsDatabaseException;
import io.github.eisop.opsc.log.SchemaTimingLogger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.avatica.util.Quoting;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelVisitor;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexDynamicParam;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.rex.RexSubQuery;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.type.SqlTypeFamily;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.checkerframework.javacutil.TypeSystemError;
import org.jspecify.annotations.Nullable;

public class CalciteSchemaInfo implements SchemaInfo {

    private static final String SUB_SCHEMA_NAME = "DB_SCHEMA";
    private static final String CLASS_NAME = "CalciteSchemaInfo";

    private final SchemaTimingLogger logger;

    private final SchemaPlus rootSchema;
    private final Connection calciteConnection;

    SqlParser.Config parserConfig =
            SqlParser.config()
                    .withCaseSensitive(false)
                    .withQuoting(Quoting.DOUBLE_QUOTE)
                    .withConformance(SqlConformanceEnum.BABEL);

    public CalciteSchemaInfo(
            String databaseUrl,
            @Nullable String username,
            @Nullable String password,
            SchemaTimingLogger logger)
            throws OpsDatabaseException {
        long startTime = System.nanoTime();

        this.logger = logger;

        // Explicitly load the Calcite and Postgres JDBC drivers, so it can be used by the checker
        // when compiling the programme under test.
        try {
            Class.forName("org.apache.calcite.jdbc.Driver");
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new TypeSystemError("Could not load JDBC driver: %s", e.getMessage());
        }

        try {
            testJdbcConnection(databaseUrl, username, password);
        } catch (SQLException e) {
            throw new OpsDatabaseException(e);
        }

        long schemaSetupStart = System.nanoTime();
        try {
            this.calciteConnection = DriverManager.getConnection("jdbc:calcite:", new Properties());
            CalciteConnection conn = this.calciteConnection.unwrap(CalciteConnection.class);
            DataSource dataSource = JdbcSchema.dataSource(databaseUrl, null, username, password);
            rootSchema = conn.getRootSchema();
            Schema subSchema =
                    JdbcSchema.create(rootSchema, SUB_SCHEMA_NAME, dataSource, null, null);
            rootSchema.add(SUB_SCHEMA_NAME, subSchema);
            long schemaSetupTime = System.nanoTime() - schemaSetupStart;
            logger.logMethodTiming(CLASS_NAME, "schemaSetup", schemaSetupTime, true, databaseUrl);
        } catch (SQLException e) {
            logger.logMethodTiming(
                    CLASS_NAME,
                    "schemaSetup",
                    System.nanoTime() - schemaSetupStart,
                    false,
                    e.getMessage());
            throw new OpsDatabaseException(e);
        }

        long totalTime = System.nanoTime() - startTime;
        logger.logMethodTiming(
                CLASS_NAME, "constructor", totalTime, true, "initialization complete");
    }

    /**
     * Tests the JDBC connection to the database by doing nothing if the connection is successful
     * and throwing an exception otherwise.
     *
     * @throws SQLException if there is a problem with the database schema or connection
     */
    private static void testJdbcConnection(
            String databaseUrl, @Nullable String username, @Nullable String password)
            throws SQLException {
        try (Connection conn = DriverManager.getConnection(databaseUrl, username, password)) {
            conn.createStatement();
        }
    }

    @Override
    public ImmutableList<String> getResultTypeOf(String stmt) throws OpsDatabaseException {
        long startTime = System.nanoTime();
        stmt = trimStatement(stmt);
        ImmutableList<String> result = getTypesWithAnnotations(parseSql(stmt).getRowType());
        long elapsedTime = System.nanoTime() - startTime;
        logger.logMethodTiming(CLASS_NAME, "getResultTypeOf", elapsedTime, true, stmt);
        return result;
    }

    private RelNode parseSql(String stmt) throws OpsDatabaseException {
        SchemaPlus subSchema = rootSchema.getSubSchema(SUB_SCHEMA_NAME);
        if (subSchema == null) {
            throw new OpsDatabaseException("Could not find sub-schema: " + SUB_SCHEMA_NAME);
        }
        FrameworkConfig frameworkConfig =
                Frameworks.newConfigBuilder()
                        .parserConfig(parserConfig)
                        .defaultSchema(subSchema)
                        .build();

        RelNode tree;
        try (Planner planner = Frameworks.getPlanner(frameworkConfig)) {
            SqlNode parsed = planner.parse(stmt);
            SqlNode validated = planner.validate(parsed);
            tree = planner.rel(validated).rel;
        } catch (ValidationException | SqlParseException | RelConversionException e) {
            throw new OpsDatabaseException(e);
        }
        return tree;
    }

    @Override
    public ImmutableList<String> getPlaceholderTypesOf(String stmt) throws OpsDatabaseException {
        long startTime = System.nanoTime();
        stmt = trimStatement(stmt);
        RelNode tree = parseSql(stmt);

        List<RexDynamicParam> params = findDynamicParams(tree);
        ImmutableList<String> result =
                params.stream()
                        .sorted(Comparator.comparingInt(RexDynamicParam::getIndex))
                        .map(param -> getJDBCTypeName(param.getType()))
                        .collect(ImmutableList.toImmutableList());

        long elapsedTime = System.nanoTime() - startTime;
        logger.logMethodTiming(CLASS_NAME, "getPlaceholderTypesOf", elapsedTime, true, stmt);
        return result;
    }

    private List<RexDynamicParam> findDynamicParams(RelNode tree) {
        List<RexDynamicParam> params = new ArrayList<>();
        tree.childrenAccept(
                new RelVisitor() {
                    @Override
                    public void visit(RelNode node, int ordinal, @Nullable RelNode parent) {
                        if (node instanceof RexDynamicParam param) {
                            params.add(param);
                        } else if (node instanceof Filter filter) {
                            filter.getCondition()
                                    .accept(
                                            new RexShuttle() {
                                                @Override
                                                public RexNode visitDynamicParam(
                                                        RexDynamicParam dynamicParam) {
                                                    params.add(dynamicParam);
                                                    return dynamicParam;
                                                }
                                            });

                            // check for RexCalls (includes subqueries) in the filter condition
                            if (filter.getCondition() instanceof RexSubQuery subQuery) {
                                visitSubQuery(subQuery);
                            } else if (filter.getCondition() instanceof RexCall call) {
                                handleRexCall(call);
                            }
                        } else if (node instanceof LogicalProject project) {
                            RexUtil.apply(
                                    new RexShuttle() {
                                        @Override
                                        public RexNode visitDynamicParam(
                                                RexDynamicParam dynamicParam) {
                                            params.add(dynamicParam);
                                            return dynamicParam;
                                        }
                                    },
                                    project.getProjects().toArray(new RexNode[0]));

                            // check for subqueries in the project expressions
                            for (RexNode expr : project.getProjects()) {
                                if (expr instanceof RexSubQuery subQuery) {
                                    visitSubQuery(subQuery);
                                } else if (expr instanceof RexCall call) {
                                    handleRexCall(call);
                                }
                            }
                        } else if (node instanceof RexSubQuery subQuery) {
                            subQuery.rel.childrenAccept(this);
                        } else if (node instanceof RexCall call) {
                            handleRexCall(call);
                        }
                        node.childrenAccept(this);
                    }

                    // recursive method to find and visit subqueries
                    private void handleRexCall(RexCall call) {
                        for (RexNode operand : call.getOperands()) {
                            if (operand instanceof RexSubQuery subQuery) {
                                subQuery.rel.childrenAccept(this);
                            } else if (operand instanceof RexCall subCall) {
                                handleRexCall(subCall);
                            }
                        }
                    }

                    private void visitSubQuery(RexSubQuery subQuery) {
                        subQuery.rel.childrenAccept(this);
                    }
                });
        return params;
    }

    private static String trimStatement(String stmt) {
        // remove trailing semicolon (and any whitespace)
        stmt = stmt.stripTrailing().replaceAll(";$", "");
        return stmt;
    }

    private ImmutableList<String> getTypesWithAnnotations(RelDataType relType) {
        return relType.getFieldList().stream()
                .map(field -> getTypeWithAnnotations(field.getType(), field.getName()))
                .collect(ImmutableList.toImmutableList());
    }

    private String getTypeWithAnnotations(RelDataType relType, String name) {
        String typeName = getJDBCTypeName(relType);

        String anno = relType.isNullable() ? "@Nullable " : "@NonNull ";

        SqlTypeFamily typeFamily = relType.getSqlTypeName().getFamily();
        String familyName = typeFamily == null ? "" : typeFamily.name();
        if (familyName.equals("CHARACTER")) {
            int maxLength = relType.getPrecision();
            if (maxLength != RelDataType.PRECISION_NOT_SPECIFIED) {
                anno += "@MaxLength(" + maxLength + ") ";
            }
        }
        return anno + typeName + " " + name;
    }

    private static String getJDBCTypeName(RelDataType relType) {
        return jdbcTypeNameFromOrdinal(relType.getSqlTypeName().getJdbcOrdinal());
    }

    public void close() throws SQLException {
        long startTime = System.nanoTime();
        try {
            if (!calciteConnection.isClosed()) {
                calciteConnection.close();
            }
            logger.logMethodTiming(
                    CLASS_NAME,
                    "close",
                    System.nanoTime() - startTime,
                    true,
                    "closing calcite connection");
        } catch (SQLException e) {
            logger.logMethodTiming(
                    CLASS_NAME, "close", System.nanoTime() - startTime, false, e.getMessage());
            throw e;
        }
    }
}
