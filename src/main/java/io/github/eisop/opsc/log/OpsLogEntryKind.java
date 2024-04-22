package io.github.eisop.opsc.log;

public enum OpsLogEntryKind {
    SUPPORTED_PREPARED_STATEMENT,
    UNSUPPORTED_PREPARED_STATEMENT,
    WARNING,
    ERROR,
    USING_FALLBACK,
    USING_SQL_STRING_HEURISTIC
}
