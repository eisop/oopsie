package io.github.eisop.opsc.db;

import com.google.common.collect.ImmutableList;
import io.github.eisop.opsc.exception.OpsDatabaseException;

/** Interface for getting schema information from a database. */
public interface SchemaInfo {
    /**
     * Gets the type with annotations of each column in the result of the given SQL or
     * PreparedStatement string.
     *
     * <p>The String can contain space-separated annotations like {@code @Nullable} or
     * {@code @MaxLength(100)}, followed by the name of the Java class corresponding to the column
     * type.
     *
     * <p>For example, {@code @Nullable @MaxLength(100) String}.
     *
     * @param stmt the SQL or PreparedStatement string
     * @return the type with annotations of each column in the result
     * @throws OpsDatabaseException if there is a problem with the database schema or connection
     */
    ImmutableList<String> getResultTypeOf(String stmt) throws OpsDatabaseException;

    /**
     * Gets the type of each parameter (`?`) in the given PreparedStatement string.
     *
     * <p>The String can contain space-separated annotations like {@code @Nullable} or
     * {@code @MaxLength(100)}, followed by the name of the Java class corresponding to the
     * parameter type.
     *
     * <p>For example, {@code @Nullable @MaxLength(100) String}.
     *
     * @param stmt the PreparedStatement string
     * @return the type of each parameter
     * @throws OpsDatabaseException if there is a problem with the database schema or connection
     */
    ImmutableList<String> getPlaceholderTypesOf(String stmt) throws OpsDatabaseException;
}
