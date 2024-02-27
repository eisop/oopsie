package io.github.eisop.opsc;

/**
 * The OpsStats class is used to keep track of the number of prepared statements found in the
 * statement.
 */
public class OpsStats {

    private int numAnnotatedPreparedStatements = 0;

    private int numUnsupportedPreparedStatements = 0;

    /** Increments the count of annotated prepared statements. */
    public void recordAnnotatedPreparedStatement() {
        numAnnotatedPreparedStatements++;
    }

    /** Increments the count of unsupported or invalid prepared statements. */
    public void recordUnsupportedPreparedStatement() {
        numUnsupportedPreparedStatements++;
    }

    /**
     * This method is used to get the count of annotated prepared statements.
     *
     * @return The number of annotated prepared statements.
     */
    public int getNumAnnotatedPreparedStatements() {
        return numAnnotatedPreparedStatements;
    }

    /**
     * This method is used to get the count of unsupported or invalid prepared statements.
     *
     * @return The number of unsupported prepared statements.
     */
    public int getNumUnsupportedPreparedStatements() {
        return numUnsupportedPreparedStatements;
    }
}
