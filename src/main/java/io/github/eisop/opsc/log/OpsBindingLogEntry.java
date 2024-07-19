package io.github.eisop.opsc.log;

import java.util.List;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class OpsBindingLogEntry {

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
    @NonNull
    private final OpsLogEntryKind kind;
    @Nullable
    private final String bindingFile;
    @Nullable
    private final String bindingLine;
    @Nullable
    private final String bindingColumn;
    @Nullable
    private final String statementFile;
    @Nullable
    private final String statementLine;
    @Nullable
    private final String statementColumn;
    @Nullable
    private final String key;
    @Nullable
    private final String details;

    public OpsBindingLogEntry(
            @NonNull OpsLogEntryKind kind,
            @Nullable String bindingFile,
            @Nullable String bindingLine,
            @Nullable String bindingColumn,
            @Nullable String statementFile,
            @Nullable String statementLine,
            @Nullable String statementColumn,
            @Nullable String key,
            @Nullable String details) {
        this.kind = kind;
        this.bindingFile = bindingFile;
        this.bindingLine = bindingLine;
        this.bindingColumn = bindingColumn;
        this.statementFile = statementFile;
        this.statementLine = statementLine;
        this.statementColumn = statementColumn;
        this.key = key;
        this.details = details;
    }

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

    @NonNull
    public OpsLogEntryKind kind() {
        return kind;
    }

    @Nullable
    public String bindingFile() {
        return bindingFile;
    }

    @Nullable
    public String bindingLine() {
        return bindingLine;
    }

    @Nullable
    public String bindingColumn() {
        return bindingColumn;
    }

    @Nullable
    public String statementFile() {
        return statementFile;
    }

    @Nullable
    public String statementLine() {
        return statementLine;
    }

    @Nullable
    public String statementColumn() {
        return statementColumn;
    }

    @Nullable
    public String key() {
        return key;
    }

    @Nullable
    public String details() {
        return details;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        OpsBindingLogEntry that = (OpsBindingLogEntry) obj;
        return Objects.equals(this.kind, that.kind) &&
                Objects.equals(this.bindingFile, that.bindingFile) &&
                Objects.equals(this.bindingLine, that.bindingLine) &&
                Objects.equals(this.bindingColumn, that.bindingColumn) &&
                Objects.equals(this.statementFile, that.statementFile) &&
                Objects.equals(this.statementLine, that.statementLine) &&
                Objects.equals(this.statementColumn, that.statementColumn) &&
                Objects.equals(this.key, that.key) &&
                Objects.equals(this.details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, bindingFile, bindingLine, bindingColumn, statementFile, statementLine, statementColumn, key, details);
    }

    @Override
    public String toString() {
        return "OpsBindingLogEntry[" +
                "kind=" + kind + ", " +
                "bindingFile=" + bindingFile + ", " +
                "bindingLine=" + bindingLine + ", " +
                "bindingColumn=" + bindingColumn + ", " +
                "statementFile=" + statementFile + ", " +
                "statementLine=" + statementLine + ", " +
                "statementColumn=" + statementColumn + ", " +
                "key=" + key + ", " +
                "details=" + details + ']';
    }

}
