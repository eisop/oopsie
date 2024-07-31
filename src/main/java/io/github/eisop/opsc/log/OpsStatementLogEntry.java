package io.github.eisop.opsc.log;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class OpsStatementLogEntry {

    static final String[] STATEMENT_COLUMNS = {
            "kind",
            "statementFile",
            "statementLine",
            "statementColumns",
            "details",
            "statementString",
            "numberOfParameters"
    };
    @NonNull
    private final OpsLogEntryKind kind;
    @Nullable
    private final String statementFile;
    @Nullable
    private final String statementLine;
    @Nullable
    private final String statementColumn;
    @Nullable
    private final String details;
    @Nullable
    private final String statementString;
    @Nullable
    private final Integer numberOfParameters;

    public OpsStatementLogEntry(
            @NonNull OpsLogEntryKind kind,
            @Nullable String statementFile,
            @Nullable String statementLine,
            @Nullable String statementColumn,
            @Nullable String details,
            @Nullable String statementString,
            @Nullable Integer numberOfParameters) {
        this.kind = kind;
        this.statementFile = statementFile;
        this.statementLine = statementLine;
        this.statementColumn = statementColumn;
        this.details = details;
        this.statementString = statementString;
        this.numberOfParameters = numberOfParameters;
    }

    public List<String> values() {
        return Arrays.asList(
                kind.toString(),
                str(statementFile),
                str(statementLine),
                str(statementColumn),
                str(details),
                str(statementString),
                str(numberOfParameters));
    }

    private String str(Object value) {
        return value == null ? "" : value.toString();
    }

    @NonNull
    public OpsLogEntryKind kind() {
        return kind;
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
    public String details() {
        return details;
    }

    @Nullable
    public String statementString() {
        return statementString;
    }

    @Nullable
    public Integer numberOfParameters() {
        return numberOfParameters;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        OpsStatementLogEntry that = (OpsStatementLogEntry) obj;
        return Objects.equals(this.kind, that.kind) &&
                Objects.equals(this.statementFile, that.statementFile) &&
                Objects.equals(this.statementLine, that.statementLine) &&
                Objects.equals(this.statementColumn, that.statementColumn) &&
                Objects.equals(this.details, that.details) &&
                Objects.equals(this.statementString, that.statementString) &&
                Objects.equals(this.numberOfParameters, that.numberOfParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, statementFile, statementLine, statementColumn, details, statementString, numberOfParameters);
    }

    @Override
    public String toString() {
        return "OpsStatementLogEntry[" +
                "kind=" + kind + ", " +
                "statementFile=" + statementFile + ", " +
                "statementLine=" + statementLine + ", " +
                "statementColumn=" + statementColumn + ", " +
                "details=" + details + ", " +
                "statementString=" + statementString + ", " +
                "numberOfParameters=" + numberOfParameters + ']';
    }

}
