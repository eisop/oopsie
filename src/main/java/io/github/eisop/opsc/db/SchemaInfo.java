package io.github.eisop.opsc.db;

import com.google.common.collect.ImmutableList;
import io.github.eisop.opsc.exception.OpsDatabaseException;

public interface SchemaInfo {
    ImmutableList<String> getResultTypeOf(String stmt) throws OpsDatabaseException;

    ImmutableList<String> getPlaceholderTypesOf(String stmt) throws OpsDatabaseException;
}
