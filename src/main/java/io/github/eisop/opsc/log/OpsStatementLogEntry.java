package io.github.eisop.opsc.log;

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public record OpsStatementLogEntry(
        @NonNull OpsLogEntryKind kind,
        @Nullable String statementFile,
        @Nullable String statementLine,
        @Nullable String statementColumn,
        @Nullable String details) {

    static final String[] STATEMENT_COLUMNS = {
        "kind", "statementFile", "statementLine", "statementColumns", "details"
    };

    public List<String> values() {
        return List.of(
                kind.toString(),
                str(statementFile),
                str(statementLine),
                str(statementColumn),
                str(details));
    }

    private String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
