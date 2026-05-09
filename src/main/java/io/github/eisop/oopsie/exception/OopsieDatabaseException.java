package io.github.eisop.oopsie.exception;

/** Exception thrown when there is a problem with the database schema or connection. */
public class OopsieDatabaseException extends Exception {
    private static final long serialVersionUID = -8221922431294045513L;

    public OopsieDatabaseException(String message) {
        super(message);
    }

    public OopsieDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public OopsieDatabaseException(Throwable cause) {
        super(cause);
    }
}
