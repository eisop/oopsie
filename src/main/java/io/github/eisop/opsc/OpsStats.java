package io.github.eisop.opsc;

public class OpsStats {

    private int numAnnotatedPreparedStatements = 0;

    private int numUnsupportedPreparedStatements = 0;

    public void recordAnnotatedPreparedStatement() {
        numAnnotatedPreparedStatements++;
    }

    public void recordUnsupportedPreparedStatement() {
        numUnsupportedPreparedStatements++;
    }

    public int getNumAnnotatedPreparedStatements() {
        return numAnnotatedPreparedStatements;
    }

    public int getNumUnsupportedPreparedStatements() {
        return numUnsupportedPreparedStatements;
    }
}
