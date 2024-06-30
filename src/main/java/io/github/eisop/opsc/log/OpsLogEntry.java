package io.github.eisop.opsc.log;

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public record OpsLogEntry(
        @NonNull OpsLogEntryKind kind,
        @Nullable String file,
        @Nullable String location,
        @Nullable String relatedStatementFile,
        @Nullable String relatedStatementLocation,
        @Nullable String key,
        @Nullable String details) {

    public List<String> statementValues() {
        return List.of(kind.toString(), str(file), str(location), str(details));
    }

    public List<String> bindingValues() {
        return List.of(
                kind.toString(),
                str(file),
                str(location),
                str(relatedStatementFile),
                str(relatedStatementLocation),
                str(key),
                str(details));
    }

    private String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
