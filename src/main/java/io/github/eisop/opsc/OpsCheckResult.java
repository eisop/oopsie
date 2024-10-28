package io.github.eisop.opsc;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class OpsCheckResult {

    private final @NonNull OpsCheckResultKind kind;

    private final @Nullable String details;

    public OpsCheckResult(@NonNull OpsCheckResultKind kind, @Nullable String key) {
        this.kind = kind;
        this.details = key;
    }

    public @NonNull OpsCheckResultKind getKind() {
        return kind;
    }

    public @Nullable String getDetails() {
        return details;
    }
}
