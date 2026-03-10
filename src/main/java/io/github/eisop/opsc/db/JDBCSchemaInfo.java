package io.github.eisop.opsc.db;

import com.google.common.collect.ImmutableList;
import io.github.eisop.opsc.exception.OpsDatabaseException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Objects;

import io.github.eisop.opsc.log.SchemaTimingLogger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TypeSystemError;

public class JDBCSchemaInfo implements SchemaInfo {

    private static final String CLASS_NAME = "JDBCSchemaInfo";

    private final SchemaTimingLogger logger;

    private final Connection connection;

    public JDBCSchemaInfo(String databaseUrl, @Nullable String username, @Nullable String password, SchemaTimingLogger logger)
            throws OpsDatabaseException {
        long startTime = System.nanoTime();

        this.logger = logger;

        // Explicitly load the PostgreSQL driver, so it can be used by the checker when compiling
        // the programme under test
        try {
            Class.forName("org.postgresql.Driver");
            // mysql
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new TypeSystemError(e.getMessage());
        }

        try {
            this.connection = DriverManager.getConnection(databaseUrl, username, password);
        } catch (SQLException e) {
            throw new OpsDatabaseException(e);
        }

        long totalTime = System.nanoTime() - startTime;
        logger.logMethodTiming(
                CLASS_NAME, "constructor", totalTime, true, "initialization complete");
    }

    @Override
    public ImmutableList<String> getResultTypeOf(String stmt) throws OpsDatabaseException {
        long startTime = System.nanoTime();
        try (PreparedStatement ps = connection.prepareStatement(stmt)) {
            ResultSetMetaData md = ps.getMetaData();
            if (md == null) {
                logger.logMethodTiming(
                        CLASS_NAME, "getResultTypeOf", System.nanoTime() - startTime, true, stmt);
                return ImmutableList.of();
            }
            ImmutableList.Builder<String> builder = ImmutableList.builder();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                builder.add(getTypeWithAnnotations(i, md));
            }
            ImmutableList<String> result = builder.build();
            logger.logMethodTiming(
                    CLASS_NAME, "getResultTypeOf", System.nanoTime() - startTime, true, stmt);
            return result;
        } catch (SQLException e) {
            throw new OpsDatabaseException(e);
        }
    }

    @Override
    public ImmutableList<String> getPlaceholderTypesOf(String stmt) throws OpsDatabaseException {
        long startTime = System.nanoTime();
        try {
            try (PreparedStatement ps = connection.prepareStatement(stmt)) {
                ParameterMetaData md = ps.getParameterMetaData();
                if (md == null) {
                    logger.logMethodTiming(
                            CLASS_NAME, "getPlaceholderTypesOf", System.nanoTime() - startTime, true, stmt);
                    return ImmutableList.of();
                }
                ImmutableList.Builder<String> builder = ImmutableList.builder();
                for (int i = 1; i <= md.getParameterCount(); i++) {
                    builder.add(getTypeWithAnnotations(i, md));
                }
                ImmutableList<String> result = builder.build();
                logger.logMethodTiming(
                        CLASS_NAME, "getPlaceholderTypesOf", System.nanoTime() - startTime, true, stmt);
                return result;
            }
        } catch (SQLException e) {
            logger.logMethodTiming(
                    CLASS_NAME,
                    "getPlaceholderTypesOf",
                    System.nanoTime() - startTime,
                    false,
                    e.getMessage());
            throw new OpsDatabaseException(e);
        }
    }

    // use the isNullable method of the given ResultSetMetaData OR ParameterMetaData
    private String getTypeWithAnnotations(int index, ResultSetMetaData md)
            throws OpsDatabaseException {
        try {
            String jdbcType = JDBCUtil.jdbcTypeNameFromOrdinal(md.getColumnType(index));
            return getTypeWithAnnotations(jdbcType, md.isNullable(index), md.getPrecision(index));
        } catch (SQLException e) {
            throw new OpsDatabaseException(e);
        }
    }

    private String getTypeWithAnnotations(int index, ParameterMetaData md)
            throws OpsDatabaseException {
        try {
            String jdbcType = JDBCUtil.jdbcTypeNameFromOrdinal(md.getParameterType(index));
            return getTypeWithAnnotations(jdbcType, md.isNullable(index), md.getPrecision(index));
        } catch (SQLException e) {
            throw new OpsDatabaseException(e);
        }
    }

    private String getTypeWithAnnotations(String className, int nullability, int precision) {
        String anno;
        switch (nullability) {
            case ParameterMetaData.parameterNoNulls:
                anno = "@NonNull ";
                break;
            case ParameterMetaData.parameterNullable:
                anno = "@Nullable ";
                break;
            case ParameterMetaData.parameterNullableUnknown:
                anno = "";
                break;
            default:
                throw new IllegalArgumentException(
                        "nullability must be one of ParameterMetaData.parameterNoNulls, "
                                + "ParameterMetaData.parameterNullable "
                                + "or ParameterMetaData.parameterNullableUnknown");
        }
        if (Objects.equals(className, "VARCHAR")) {
            if (precision != 0) {
                anno += "@MaxLength(" + precision + ") ";
            }
        }
        return anno + className;
    }

    public void close() throws SQLException {
        long startTime = System.nanoTime();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            logger.logMethodTiming(
                    CLASS_NAME,
                    "close",
                    System.nanoTime() - startTime,
                    true,
                    "closing jdbc connection");
        } catch (SQLException e) {
            logger.logMethodTiming(
                    CLASS_NAME, "close", System.nanoTime() - startTime, false, e.getMessage());
            throw e;
        }
    }
}
