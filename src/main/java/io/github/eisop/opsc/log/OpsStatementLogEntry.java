package io.github.eisop.opsc.log;

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public record OpsStatementLogEntry(
        @NonNull OpsLogEntryKind kind,
        @Nullable String statementFile,
        @Nullable String statementLine,
        @Nullable String statementColumn,
        @Nullable String details,
        @Nullable String statementString,
        @Nullable Integer numberOfParameters,
        @Nullable Boolean isPreparedStatement) {

    static final String[] STATEMENT_COLUMNS = {
        "kind",
        "statementFile",
        "statementLine",
        "statementColumns",
        "details",
        "statementString",
        "numberOfParameters",
        "isPreparedStatement"
    };

    public List<String> values() {
        return List.of(
                kind.toString(),
                str(statementFile),
                str(statementLine),
                str(statementColumn),
                str(details),
                str(statementString),
                str(numberOfParameters),
                str(isPreparedStatement));
    }

    private String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
