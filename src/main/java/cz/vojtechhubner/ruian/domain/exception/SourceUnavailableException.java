package cz.vojtechhubner.ruian.domain.exception;

import java.net.URI;

/**
 * The source could not be downloaded or read: an HTTP error, a network failure or a malformed archive.
 */
public class SourceUnavailableException extends ImportException {

    private final URI source;

    public SourceUnavailableException(URI source, String reason) {
        super(message(source, reason));
        this.source = source;
    }

    public SourceUnavailableException(URI source, String reason, Throwable cause) {
        super(message(source, reason), cause);
        this.source = source;
    }

    private static String message(URI source, String reason) {
        return "Source " + source + " is unavailable: " + reason;
    }

    public URI source() {
        return source;
    }
}
