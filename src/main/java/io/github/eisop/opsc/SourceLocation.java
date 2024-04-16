package io.github.eisop.opsc;

import com.sun.source.tree.CompilationUnitTree;

public record SourceLocation(
        CompilationUnitTree compilationUnit,
        long start
) {
}
