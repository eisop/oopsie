package io.github.eisop.opsc.db;

import com.google.common.collect.ImmutableList;
import io.github.eisop.opsc.exception.OpsDatabaseException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
import org.apache.calcite.rex.RexDynamicParam;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TypeSystemError;

public class CalciteSchemaInfo implements SchemaInfo {

    private static final String SUB_SCHEMA_NAME = "DB_SCHEMA";

    private final SchemaPlus rootSchema;

    SqlParser.Config parserConfig =
            SqlParser.config().withCaseSensitive(false).withQuoting(Quoting.DOUBLE_QUOTE);

    public CalciteSchemaInfo(
            String databaseUrl, @Nullable String username, @Nullable String password)
            throws OpsDatabaseException {
        // Explicitly load the Calcite and Postgres JDBC drivers, so it can be used by the checker
        // when compiling the programme under test.
        try {
            Class.forName("org.apache.calcite.jdbc.Driver");
            Class.forName("org.postgresql.Driver");
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new TypeSystemError(e.getMessage());
        }

        try {
            testJdbcConnection(databaseUrl, username, password);
            System.out.println("[CalciteSchemaInfo] JDBC connection to " + databaseUrl + " OK");
        } catch (SQLException e) {
            throw new OpsDatabaseException(e);
        }

        CalciteConnection calciteConnection;
        try {
            Connection conn = DriverManager.getConnection("jdbc:calcite:", new Properties());
            calciteConnection = conn.unwrap(CalciteConnection.class);
        } catch (SQLException e) {
            throw new OpsDatabaseException(e);
        }
        DataSource dataSource = JdbcSchema.dataSource(databaseUrl, null, username, password);
        rootSchema = calciteConnection.getRootSchema();
        Schema subSchema = JdbcSchema.create(rootSchema, SUB_SCHEMA_NAME, dataSource, null, null);
        rootSchema.add(SUB_SCHEMA_NAME, subSchema);
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
        //        System.out.println(parseSql(stmt).getRowType().getFieldNames());
        //        System.out.println(parseSql(stmt).getRowType().getFieldList());
        //        System.out.println(parseSql(stmt).getRowType().getFieldCount());
        return getJavaTypesWithAnnotations(parseSql(stmt).getRowType());
    }

    @Override
    public ImmutableList<String> getPlaceholderTypesOf(String stmt) throws OpsDatabaseException {
        RelNode tree = parseSql(stmt);
        List<RexDynamicParam> params = new ArrayList<>();
        tree.childrenAccept(
                new RelVisitor() {
                    @Override
                    public void visit(RelNode node, int ordinal, @Nullable RelNode parent) {
                        if (node instanceof RexDynamicParam) {
                            RexDynamicParam param = (RexDynamicParam) node;
                            params.add(param);
                        } else if (node instanceof Filter) {
                            Filter filter = (Filter) node;
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
                        } else if (node instanceof LogicalProject) {
                            LogicalProject project = (LogicalProject) node;
                            project.getProjects()
                                    .forEach(
                                            p ->
                                                    p.accept(
                                                            new RexShuttle() {
                                                                @Override
                                                                public RexNode visitDynamicParam(
                                                                        RexDynamicParam
                                                                                dynamicParam) {
                                                                    params.add(dynamicParam);
                                                                    return dynamicParam;
                                                                }
                                                            }));
                        }
                        node.childrenAccept(this);
                    }
                });

        return params.stream()
                .sorted(Comparator.comparingInt(RexDynamicParam::getIndex))
                .map(param -> getJavaType(param.getType()))
                .collect(ImmutableList.toImmutableList());
    }

    private RelNode parseSql(String stmt) throws OpsDatabaseException {
        FrameworkConfig frameworkConfig =
                Frameworks.newConfigBuilder()
                        .parserConfig(parserConfig)
                        .defaultSchema(rootSchema.getSubSchema(SUB_SCHEMA_NAME))
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

    private ImmutableList<String> getJavaTypesWithAnnotations(RelDataType relType) {
        return relType.getFieldList().stream()
                .map(field -> getJavaTypeWithAnnotations(field.getType()))
                .collect(ImmutableList.toImmutableList());
    }

    private String getJavaTypeWithAnnotations(RelDataType relType) {
        String type = getJavaType(relType);
        String anno = relType.isNullable() ? "@Nullable " : "@NonNull ";
        if (Objects.equals(type, "String")) {
            int maxLength = relType.getPrecision();
            if (maxLength != RelDataType.PRECISION_NOT_SPECIFIED) {
                anno += "@MaxLength(" + maxLength + ") ";
            }
        }
        return anno + type;
    }

    private String getJavaType(RelDataType relType) {
        SqlTypeName sqlTypeName = relType.getSqlTypeName();
        if (sqlTypeName.getFamily() == null) {
            throw new RuntimeException("Unknown SQL type: " + sqlTypeName);
        }

        //        return switch (sqlTypeName.getFamily()) {
        //            case CHARACTER -> "String";
        //            case DATE -> "Date";
        //            case TIME -> "Time";
        //            case TIMESTAMP -> "Timestamp";
        //            case BOOLEAN -> "Boolean";
        //            case NUMERIC -> switch (sqlTypeName) {
        //                case INTEGER, TINYINT, SMALLINT, BIGINT -> "Integer";
        //                case DECIMAL -> "BigDecimal";
        //                default -> "Double";
        //            };
        //            default -> "Object";
        //        };
        // java8
        String type;
        switch (sqlTypeName.getFamily()) {
            case CHARACTER:
                type = "String";
                break;
            case DATE:
                type = "Date";
                break;
            case TIME:
                type = "Time";
                break;
            case TIMESTAMP:
                type = "Timestamp";
                break;
            case BOOLEAN:
                type = "Boolean";
                break;
            case NUMERIC:
                switch (sqlTypeName) {
                    case INTEGER:
                    case TINYINT:
                    case SMALLINT:
                    case BIGINT:
                        type = "Integer";
                        break;
                    case DECIMAL:
                        type = "BigDecimal";
                        break;
                    default:
                        type = "Double";
                        break;
                }
                break;
            default:
                type = "Object";
                break;
        }
        return type;
    }
}
