package cz.vojtechhubner.ruian.domain.exception;

/**
 * Base class of everything that can go wrong while importing a municipality.
 */
public class ImportException extends RuntimeException {

    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
