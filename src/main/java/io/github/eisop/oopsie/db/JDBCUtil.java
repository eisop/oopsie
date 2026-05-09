package io.github.eisop.oopsie.db;

import java.sql.Types;
import org.checkerframework.javacutil.TypeSystemError;

public final class JDBCUtil {

    private JDBCUtil() {
        throw new UnsupportedOperationException("This class should not be instantiated");
    }

    public static String jdbcTypeNameFromOrdinal(int ordinal) {
        return switch (ordinal) {
            case Types.BIT -> "BIT";
            case Types.TINYINT -> "TINYINT";
            case Types.SMALLINT -> "SMALLINT";
            case Types.INTEGER -> "INTEGER";
            case Types.BIGINT -> "BIGINT";
            case Types.FLOAT -> "FLOAT";
            case Types.REAL -> "REAL";
            case Types.DOUBLE -> "DOUBLE";
            case Types.NUMERIC -> "NUMERIC";
            case Types.DECIMAL -> "DECIMAL";
            case Types.CHAR -> "CHAR";
            case Types.VARCHAR -> "VARCHAR";
            case Types.LONGVARCHAR -> "LONGVARCHAR";
            case Types.DATE -> "DATE";
            case Types.TIME -> "TIME";
            case Types.TIMESTAMP -> "TIMESTAMP";
            case Types.BINARY -> "BINARY";
            case Types.VARBINARY -> "VARBINARY";
            case Types.LONGVARBINARY -> "LONGVARBINARY";
            case Types.NULL -> "NULL";
            case Types.OTHER -> "OTHER";
            case Types.JAVA_OBJECT -> "JAVA_OBJECT";
            case Types.DISTINCT -> "DISTINCT";
            case Types.STRUCT -> "STRUCT";
            case Types.ARRAY -> "ARRAY";
            case Types.BLOB -> "BLOB";
            case Types.CLOB -> "CLOB";
            case Types.REF -> "REF";
            case Types.DATALINK -> "DATALINK";
            case Types.BOOLEAN -> "BOOLEAN";
            case Types.ROWID -> "ROWID";
            case Types.NCHAR -> "NCHAR";
            case Types.NVARCHAR -> "NVARCHAR";
            case Types.LONGNVARCHAR -> "LONGNVARCHAR";
            case Types.NCLOB -> "NCLOB";
            case Types.SQLXML -> "SQLXML";
            case Types.REF_CURSOR -> "REF_CURSOR";
            case Types.TIME_WITH_TIMEZONE -> "TIME_WITH_TIMEZONE";
            case Types.TIMESTAMP_WITH_TIMEZONE -> "TIMESTAMP_WITH_TIMEZONE";
            default -> throw new TypeSystemError("Unknown JDBC type");
        };
    }
}
