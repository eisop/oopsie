package io.github.eisop.opsc.log;

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public record OpsLogEntry(
        @NonNull OpsLogEntryKind kind,
        @NonNull String file,
        @NonNull Long location,
        @Nullable String relatedStatementFile,
        @Nullable Long relatedStatementLocation,
        @Nullable String key,
        @Nullable String details) {

    public List<String> values() {
        return List.of(
                kind.toString(),
                file,
                location.toString(),
                str(relatedStatementFile),
                str(relatedStatementLocation),
                str(key),
                str(details));
    }

    private String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
