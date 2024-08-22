package io.github.eisop.opsc;

import com.google.common.base.Splitter;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record OpscType(
        String columnDataType, List<String> columnAnnotations, @Nullable String columnName) {

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
        if (!(obj instanceof OpscType other)) {
            return false;
        }
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
}
