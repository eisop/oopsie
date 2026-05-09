package io.github.eisop.oopsie;

public class OopsieCheckResult {

    private final OopsieCheckResultKind kind;

    private final String details;

    public OopsieCheckResult(OopsieCheckResultKind kind, String key) {
        this.kind = kind;
        this.details = key;
    }

    public OopsieCheckResultKind getKind() {
        return kind;
    }

    public String getDetails() {
        return details;
    }
}
