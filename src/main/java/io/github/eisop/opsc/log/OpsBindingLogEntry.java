package io.github.eisop.opsc.log;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record OpsBindingLogEntry(
        OpsLogEntryKind kind,
        @Nullable String bindingFile,
        @Nullable String bindingLine,
        @Nullable String bindingColumn,
        @Nullable String statementFile,
        @Nullable String statementLine,
        @Nullable String statementColumn,
        @Nullable String key,
        @Nullable String details) {

    static final String[] BINDING_COLUMNS = {
        "kind",
        "bindingFile",
        "bindingLine",
        "bindingColumn",
        "statementFile",
        "statementLine",
        "statementColumn",
        "key",
        "details"
    };

    public List<String> values() {
        return List.of(
                kind.toString(),
                str(bindingFile),
                str(bindingLine),
                str(bindingColumn),
                str(statementFile),
                str(statementLine),
                str(statementColumn),
                str(key),
                str(details));
    }

    private String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
