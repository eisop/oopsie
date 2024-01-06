package io.github.eisop.opsc.exception;

/** Exception thrown when there is a problem with the database schema or connection. */
public class OpsDatabaseException extends Exception {
    private static final long serialVersionUID = -8221922431294045513L;

    public OpsDatabaseException(String message) {
        super(message);
    }

    public OpsDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpsDatabaseException(Throwable cause) {
        super(cause);
    }
}
