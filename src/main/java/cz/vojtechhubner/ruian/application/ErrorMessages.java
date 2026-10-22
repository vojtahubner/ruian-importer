package cz.vojtechhubner.ruian.application;

import cz.vojtechhubner.ruian.domain.exception.ImportException;

/**
 * Builds the error message recorded on a FAILED run. A domain failure is described by its own message; anything
 * else is reduced to its type, so that internal detail (SQL, hosts, file paths) stays in the server log.
 */
final class ErrorMessages {

    private ErrorMessages() {
    }

    static String sanitize(Throwable cause) {
        if (cause instanceof ImportException) {
            String message = cause.getMessage();
            return message != null ? message : cause.getClass().getSimpleName();
        }
        return "Unexpected " + cause.getClass().getSimpleName() + " (see server log)";
    }
}
