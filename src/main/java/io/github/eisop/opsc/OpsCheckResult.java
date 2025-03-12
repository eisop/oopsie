package io.github.eisop.opsc;

public class OpsCheckResult {

    private final OpsCheckResultKind kind;

    private final String details;

    public OpsCheckResult(OpsCheckResultKind kind, String key) {
        this.kind = kind;
        this.details = key;
    }

    public OpsCheckResultKind getKind() {
        return kind;
    }

    public String getDetails() {
        return details;
    }
}
