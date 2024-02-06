package io.github.eisop.opsc;

import com.google.common.base.Splitter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class OpscType {
    @NonNull
    private final String columnDataType;
    @NonNull
    private final List<String> columnAnnotations;
    @Nullable
    private final String columnName;

    public OpscType(
            @NonNull String columnDataType,
            @NonNull List<String> columnAnnotations,
            @Nullable String columnName) {
        this.columnDataType = columnDataType;
        this.columnAnnotations = columnAnnotations;
        this.columnName = columnName;
    }

    public static OpscType fromAnnotationString(String annotationString) {
        String columnDataType;
        List<String> columnAnnotations;
        String columnName;

        List<String> tokens = Splitter.on(' ').splitToList(annotationString);

        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Annotation string must not be empty");
        }

        if (tokens.size() == 1) {
            columnDataType = tokens.get(0);
            columnAnnotations = Collections.emptyList();
            columnName = null;
        } else if (tokens.get(tokens.size() - 2).startsWith("@")) {
            columnDataType = tokens.get(tokens.size() - 1);
            columnAnnotations = tokens.subList(0, tokens.size() - 1);
            columnName = null;
        } else {
            columnDataType = tokens.get(tokens.size() - 2);
            columnAnnotations = tokens.subList(0, tokens.size() - 2);
            columnName = tokens.get(tokens.size() - 1);
        }

        return new OpscType(columnDataType, columnAnnotations, columnName);
    }

    public boolean dataTypeMatches(OpscType other, boolean ignoreCase) {
        if (ignoreCase) {
            return columnDataType.equalsIgnoreCase(other.columnDataType);
        } else {
            return columnDataType.equals(other.columnDataType);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OpscType)) {
            return false;
        }
        OpscType other = (OpscType) obj;
        if (this == obj) {
            return true;
        }

        if (columnName == null) {
            return equalsIgnoringName(other, true) && other.columnName == null;
        }

        return dataTypeMatches(other, true)
               && columnAnnotations.equals(other.columnAnnotations)
               && other.columnName != null
               && columnName.equalsIgnoreCase(other.columnName);
    }

    public boolean equalsIgnoringName(OpscType other, boolean ignoreCase) {
        return dataTypeMatches(other, ignoreCase)
               && columnAnnotations.equals(other.columnAnnotations);
    }

    @Override
    public String toString() {
        String annotationString = String.join(" ", columnAnnotations);
        if (!columnAnnotations.isEmpty()) {
            annotationString = annotationString + " ";
        }
        annotationString = annotationString + columnDataType;
        if (columnName != null) {
            annotationString = annotationString + " " + columnName;
        }
        return annotationString;
    }

    @NonNull
    public String columnDataType() {
        return columnDataType;
    }

    @NonNull
    public List<String> columnAnnotations() {
        return columnAnnotations;
    }

    @Nullable
    public String columnName() {
        return columnName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnDataType, columnAnnotations, columnName);
    }

}
