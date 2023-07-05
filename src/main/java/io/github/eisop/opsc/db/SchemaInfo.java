package io.github.eisop.opsc.db;

import com.google.common.collect.ImmutableList;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.avatica.util.Quoting;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.*;

public class SchemaInfo {

    private static final String SUB_SCHEMA_NAME = "DB_SCHEMA";

    private final SchemaPlus rootSchema;

    CalciteConnection calciteConnection;

    SqlParser.Config parserConfig =
            SqlParser.config().withCaseSensitive(false).withQuoting(Quoting.DOUBLE_QUOTE);

    public SchemaInfo(String databaseUrl, String username, String password) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:calcite:", new Properties());
        calciteConnection = conn.unwrap(CalciteConnection.class);
        DataSource dataSource = JdbcSchema.dataSource(databaseUrl, null, username, password);
        rootSchema = calciteConnection.getRootSchema();
        Schema subSchema = JdbcSchema.create(rootSchema, SUB_SCHEMA_NAME, dataSource, null, null);
        rootSchema.add(SUB_SCHEMA_NAME, subSchema);
    }

    public ImmutableList<String> getResultTypeOf(String stmt) {
        FrameworkConfig frameworkConfig =
                Frameworks.newConfigBuilder()
                        .parserConfig(parserConfig)
                        .defaultSchema(rootSchema.getSubSchema(SUB_SCHEMA_NAME))
                        .build();
        try (Planner planner = Frameworks.getPlanner(frameworkConfig)) {
            SqlNode parsed = planner.parse(stmt);
            SqlNode validated = planner.validate(parsed);
            RelNode relNode = planner.rel(validated).rel;

            return getJavaTypes(relNode.getRowType());
        } catch (ValidationException | SqlParseException | RelConversionException e) {
            return null;
        }
    }

    private ImmutableList<String> getJavaTypes(RelDataType relType) {
        return relType.getFieldList().stream()
                .map(field -> getJavaType(field.getType()))
                .collect(ImmutableList.toImmutableList());
    }

    private String getJavaType(RelDataType relType) {
        String nullableAnno = relType.isNullable() ? "@Nullable " : "@NonNull ";

        SqlTypeName sqlTypeName = relType.getSqlTypeName();
        if (sqlTypeName.getFamily() == null) {
            return nullableAnno + "Object";
        }

        return nullableAnno
                + switch (sqlTypeName.getFamily()) {
                    case CHARACTER -> {
                        int maxLength = relType.getPrecision();
                        String maxLengthAnno =
                                maxLength == RelDataType.PRECISION_NOT_SPECIFIED
                                        ? ""
                                        : "@MaxLength(" + maxLength + ") ";
                        yield maxLengthAnno + "String";
                    }
                    case DATE -> "Date";
                    case TIME -> "Time";
                    case TIMESTAMP -> "Timestamp";
                    case BOOLEAN -> "Boolean";
                    case NUMERIC -> switch (relType.getSqlTypeName()) {
                        case INTEGER, TINYINT, SMALLINT, BIGINT -> "Integer";
                        default -> "Double";
                    };
                    default -> "Object";
                };
    }
}
