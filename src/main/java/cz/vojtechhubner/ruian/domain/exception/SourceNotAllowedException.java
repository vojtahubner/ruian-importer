package cz.vojtechhubner.ruian.domain.exception;

import java.net.URI;

/**
 * The requested source is rejected by the import policy (for example its host is not allowed) before anything is
 * downloaded.
 */
public class SourceNotAllowedException extends ImportException {

    private final URI source;

    public SourceNotAllowedException(URI source, String reason) {
        super("Source " + source + " is not allowed: " + reason);
        this.source = source;
    }

    public URI source() {
        return source;
    }
}
