package io.github.eisop.oopsie;

import com.google.common.base.Splitter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class OopsieType {
    @NonNull
    private final String columnDataType;
    @Nullable
    private final String columnName;

    public OopsieType(
            @NonNull String columnDataType, @Nullable String columnName) {
        this.columnDataType = columnDataType;
        this.columnName = columnName;
    }

    /**
     * !FOR NOW, THE AT-SIGN ANNOTATIONS ARE IGNORED!
     *
     * <p>Parses an annotation string and constructs an {@code oopsieType} instance based on the
     * parsed content. The input string is expected to contain a column type, an optional list of
     * annotations, and an optional column name.
     *
     * <p>The annotations begin with the {@code @} symbol and are separated by spaces. Example
     * strings could be: "@MaxLength(10) @Nullable VARCHAR name", "INTEGER age", or "DATE".
     *
     * @param annotationString the string containing type, optional annotations, and optionally the
     *     column name.
     * @return an {@code oopsieType} instance containing the parsed type, annotations, and column
     *     name information.
     * @throws IllegalArgumentException if the input string is empty.
     */
    public static OopsieType fromAnnotationString(String annotationString) {
        String columnDataType;
//        List<String> columnAnnotations;
        String columnName;

        List<String> tokens = Splitter.on(' ').splitToList(annotationString);

        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Annotation string must not be empty");
        }

        if (tokens.size() == 1) {
            columnDataType = tokens.get(0);
//            columnAnnotations = Collections.emptyList();
            columnName = null;
        } else if (tokens.get(tokens.size() - 2).startsWith("@")) {
            columnDataType = tokens.get(tokens.size() - 1);
//            columnAnnotations = tokens.subList(0, tokens.size() - 1);
            columnName = null;
        } else {
            columnDataType = tokens.get(tokens.size() - 2);
//            columnAnnotations = tokens.subList(0, tokens.size() - 2);
            columnName = tokens.get(tokens.size() - 1);
        }

        return new OopsieType(columnDataType, columnName);
    }

    public boolean dataTypeMatches(OopsieType other) {
        if (columnDataType.equals(other.columnDataType)) {
            return true;
        }
        return (columnDataType.equals("NUMERIC") && other.columnDataType.equals("DECIMAL"))
                || (columnDataType.equals("DECIMAL") && other.columnDataType.equals("NUMERIC"));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OopsieType)) {
            return false;
        }
        OopsieType other = (OopsieType) obj;
        if (this == obj) {
            return true;
        }

        if (columnName == null) {
            return equalsIgnoringName(other) && other.columnName == null;
        }

        return dataTypeMatches(other)
                && other.columnName != null
                && columnName.equalsIgnoreCase(other.columnName);
    }

    public boolean equalsIgnoringName(OopsieType other) {
        return dataTypeMatches(other);
    }

    @Override
    public String toString() {
        String annotationString = columnDataType;
        if (columnName != null) {
            annotationString = annotationString + " " + columnName;
        }
        return annotationString;
    }

    @NonNull
    public String columnDataType() {
        return columnDataType;
    }

    @Nullable
    public String columnName() {
        return columnName;
    }
}
