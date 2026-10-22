package cz.vojtechhubner.ruian.domain.exception;

/**
 * The VFR XML could not be parsed into a municipality and its parts.
 */
public class VfrParseException extends ImportException {

    public VfrParseException(String message) {
        super(message);
    }

    public VfrParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
