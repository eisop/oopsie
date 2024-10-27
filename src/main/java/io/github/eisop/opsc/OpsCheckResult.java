package io.github.eisop.opsc;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class OpsCheckResult {

    private final @NonNull OpsCheckResultKind kind;

    private final @Nullable String key;

    public OpsCheckResult(@NonNull OpsCheckResultKind kind, @Nullable String key) {
        this.kind = kind;
        this.key = key;
    }

    public @NonNull OpsCheckResultKind getKind() {
        return kind;
    }

    public @Nullable String getKey() {
        return key;
    }
}
