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

public class CalciteSchemaInfo implements SchemaInfo {

    private static final String SUB_SCHEMA_NAME = "DB_SCHEMA";

    private final SchemaPlus rootSchema;

    SqlParser.Config parserConfig =
            SqlParser.config().withCaseSensitive(false).withQuoting(Quoting.DOUBLE_QUOTE);

    public CalciteSchemaInfo(
            String databaseUrl, @Nullable String username, @Nullable String password)
            throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:calcite:", new Properties());
        CalciteConnection calciteConnection = conn.unwrap(CalciteConnection.class);
        DataSource dataSource = JdbcSchema.dataSource(databaseUrl, null, username, password);
        rootSchema = calciteConnection.getRootSchema();
        Schema subSchema = JdbcSchema.create(rootSchema, SUB_SCHEMA_NAME, dataSource, null, null);
        rootSchema.add(SUB_SCHEMA_NAME, subSchema);
    }

    @Override
    public ImmutableList<String> getResultTypeOf(String stmt) throws OpsDatabaseException {
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
            throw new OpsDatabaseException(
                    "Could not extract the result type of the SQL statement:\n" + stmt, e);
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

        return switch (sqlTypeName.getFamily()) {
            case CHARACTER -> "String";
            case DATE -> "Date";
            case TIME -> "Time";
            case TIMESTAMP -> "Timestamp";
            case BOOLEAN -> "Boolean";
            case NUMERIC -> switch (sqlTypeName) {
                case INTEGER, TINYINT, SMALLINT, BIGINT -> "Integer";
                default -> "Double";
            };
            default -> "Object";
        };
    }
}
