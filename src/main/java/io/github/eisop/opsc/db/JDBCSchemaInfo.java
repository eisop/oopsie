package io.github.eisop.opsc.db;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import io.github.eisop.opsc.exception.OpsDatabaseException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

public class JDBCSchemaInfo implements SchemaInfo {

    private final String databaseUrl;
    private final @Nullable String username;
    private final @Nullable String password;

    public JDBCSchemaInfo(
            String databaseUrl, @Nullable String username, @Nullable String password) {
        this.databaseUrl = databaseUrl;
        this.username = username;
        this.password = password;
    }

    @Override
    public ImmutableList<String> getResultTypeOf(String stmt) throws OpsDatabaseException {
        // Explicitly load the PostgreSQL driver, so it can be used by the checker when compiling
        // the programme under test
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        try (Connection conn = DriverManager.getConnection(databaseUrl, username, password)) {
            PreparedStatement ps = conn.prepareStatement(stmt);
            ResultSetMetaData md = ps.getMetaData();
            ImmutableList.Builder<String> builder = ImmutableList.builder();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                builder.add(getJavaTypeWithAnnotations(i, md));
            }
            return builder.build();
        } catch (SQLException e) {
            throw new OpsDatabaseException(e);
        }
    }

    @Override
    public ImmutableList<String> getPlaceholderTypesOf(String stmt) throws OpsDatabaseException {
        // Explicitly load the PostgreSQL driver, so it can be used by the checker when compiling
        // the programme under test
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        try (Connection conn = DriverManager.getConnection(databaseUrl, username, password)) {
            PreparedStatement ps = conn.prepareStatement(stmt);
            ParameterMetaData md = ps.getParameterMetaData();
            ImmutableList.Builder<String> builder = ImmutableList.builder();
            for (int i = 1; i <= md.getParameterCount(); i++) {
                builder.add(getJavaTypeWithAnnotations(i, md));
            }
            return builder.build();
        } catch (SQLException e) {
            throw new OpsDatabaseException(e);
        }
    }

    // use the isNullable method of the given ResultSetMetaData OR ParameterMetaData
    private String getJavaTypeWithAnnotations(int index, ResultSetMetaData md)
            throws OpsDatabaseException {
        try {
            return getJavaTypeWithAnnotations(
                    classNameFromFullyQualified(md.getColumnClassName(index)),
                    md.isNullable(index),
                    md.getPrecision(index));
        } catch (SQLException e) {
            throw new OpsDatabaseException(e);
        }
    }

    private String getJavaTypeWithAnnotations(int index, ParameterMetaData md)
            throws OpsDatabaseException {
        try {
            return getJavaTypeWithAnnotations(
                    classNameFromFullyQualified(md.getParameterClassName(index)),
                    md.isNullable(index),
                    md.getPrecision(index));
        } catch (SQLException e) {
            throw new OpsDatabaseException(e);
        }
    }

    private String classNameFromFullyQualified(String fullyQualifiedName) {
        List<String> splits = Splitter.on(".").splitToList(fullyQualifiedName);
        return splits.get(splits.size() - 1);
    }

    private String getJavaTypeWithAnnotations(String className, int nullability, int precision) {
        String anno =
                switch (nullability) {
                    case ParameterMetaData.parameterNoNulls -> "@NonNull ";
                    case ParameterMetaData.parameterNullable -> "@Nullable ";
                    case ParameterMetaData.parameterNullableUnknown -> "";
                    default -> throw new IllegalArgumentException(
                            "nullability must be one of ParameterMetaData.parameterNoNulls, "
                                    + "ParameterMetaData.parameterNullable "
                                    + "or ParameterMetaData.parameterNullableUnknown");
                };
        if (Objects.equals(className, "String")) {
            if (precision != 0) {
                anno += " @MaxLength(" + precision + ") ";
            }
        }
        return anno + className;
    }
}
