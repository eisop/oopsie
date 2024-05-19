package io.github.eisop.opsc.log;

import java.util.List;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Record converted to class for Java 8 compatibility.
 */
public final class OpsLogEntry {
    @NonNull
    private final OpsLogEntryKind kind;
    @Nullable
    private final String file;
    @Nullable
    private final String location;
    @Nullable
    private final String relatedStatementFile;
    @Nullable
    private final String relatedStatementLocation;
    @Nullable
    private final String key;
    @Nullable
    private final String details;

    public OpsLogEntry(
            @NonNull OpsLogEntryKind kind,
            @Nullable String file,
            @Nullable String location,
            @Nullable String relatedStatementFile,
            @Nullable String relatedStatementLocation,
            @Nullable String key,
            @Nullable String details) {
        this.kind = kind;
        this.file = file;
        this.location = location;
        this.relatedStatementFile = relatedStatementFile;
        this.relatedStatementLocation = relatedStatementLocation;
        this.key = key;
        this.details = details;
    }

    public List<String> values() {
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

    @NonNull
    public OpsLogEntryKind kind() {
        return kind;
    }

    @Nullable
    public String file() {
        return file;
    }

    @Nullable
    public String location() {
        return location;
    }

    @Nullable
    public String relatedStatementFile() {
        return relatedStatementFile;
    }

    @Nullable
    public String relatedStatementLocation() {
        return relatedStatementLocation;
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
        OpsLogEntry that = (OpsLogEntry) obj;
        return Objects.equals(this.kind, that.kind) &&
                Objects.equals(this.file, that.file) &&
                Objects.equals(this.location, that.location) &&
                Objects.equals(this.relatedStatementFile, that.relatedStatementFile) &&
                Objects.equals(this.relatedStatementLocation, that.relatedStatementLocation) &&
                Objects.equals(this.key, that.key) &&
                Objects.equals(this.details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, file, location, relatedStatementFile, relatedStatementLocation, key, details);
    }

    @Override
    public String toString() {
        return "OpsLogEntry[" +
                "kind=" + kind + ", " +
                "file=" + file + ", " +
                "location=" + location + ", " +
                "relatedStatementFile=" + relatedStatementFile + ", " +
                "relatedStatementLocation=" + relatedStatementLocation + ", " +
                "key=" + key + ", " +
                "details=" + details + ']';
    }

}
