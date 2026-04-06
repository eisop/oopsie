package io.github.eisop.oopsie;

import com.google.common.base.Splitter;
import java.util.List;
import org.checkerframework.dataflow.qual.Pure;
import org.jspecify.annotations.Nullable;

public record OopsieType(String columnDataType, @Nullable String columnName) {

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

    @Pure
    public boolean dataTypeMatches(OopsieType other) {
        if (columnDataType.equals(other.columnDataType)) {
            return true;
        }
        return (columnDataType.equals("NUMERIC") && other.columnDataType.equals("DECIMAL"))
                || (columnDataType.equals("DECIMAL") && other.columnDataType.equals("NUMERIC"));
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof OopsieType other)) {
            return false;
        }
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

    @Pure
    public boolean equalsIgnoringName(OopsieType other) {
        return dataTypeMatches(other);
    }

    @Override
    public String toString() {
        if (columnName != null) {
            return columnDataType + " " + columnName;
        }
        return columnDataType;
    }
}
