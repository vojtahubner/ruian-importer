package cz.vojtechhubner.ruian.adapter.in.web;

import cz.vojtechhubner.ruian.domain.exception.ImportFailedException;
import cz.vojtechhubner.ruian.domain.exception.NotFoundException;
import cz.vojtechhubner.ruian.domain.exception.SourceNotAllowedException;
import cz.vojtechhubner.ruian.domain.exception.SourceUnavailableException;
import cz.vojtechhubner.ruian.domain.exception.VfrParseException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 problem details. The base class covers the Spring MVC failures (validation,
 * unreadable bodies, {@code ResponseStatusException}); the domain exceptions are mapped here, and anything else
 * becomes a generic 500 whose message stays in the server log.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final URI SOURCE_NOT_ALLOWED = URI.create("urn:problem-type:source-not-allowed");
    private static final URI SOURCE_UNAVAILABLE = URI.create("urn:problem-type:source-unavailable");
    private static final URI INVALID_VFR = URI.create("urn:problem-type:invalid-vfr");
    private static final URI IMPORT_FAILED = URI.create("urn:problem-type:import-failed");
    private static final URI NOT_FOUND = URI.create("urn:problem-type:not-found");
    private static final URI INTERNAL_ERROR = URI.create("urn:problem-type:internal-error");

    @ExceptionHandler(SourceNotAllowedException.class)
    public ProblemDetail handleSourceNotAllowed(SourceNotAllowedException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, SOURCE_NOT_ALLOWED, "Source not allowed", e.getMessage());
    }

    /**
     * The failed run is already recorded (and logged by the service), so the problem points at it. Only a domain
     * failure contributes its message as the detail; the exception's own message repeats the raw cause message and
     * is never exposed.
     */
    @ExceptionHandler(ImportFailedException.class)
    public ProblemDetail handleImportFailed(ImportFailedException e) {
        ProblemDetail problem = switch (e.getCause()) {
            case SourceUnavailableException cause ->
                    problem(HttpStatus.BAD_GATEWAY, SOURCE_UNAVAILABLE, "Source unavailable", cause.getMessage());
            case VfrParseException cause ->
                    problem(HttpStatus.UNPROCESSABLE_CONTENT, INVALID_VFR, "Invalid VFR data", cause.getMessage());
            case null, default -> problem(HttpStatus.INTERNAL_SERVER_ERROR, IMPORT_FAILED, "Import failed",
                    "The import failed unexpectedly; see the server log.");
        };
        problem.setInstance(URI.create("/api/imports/" + e.failedRun().id()));
        problem.setProperty("importRunId", e.failedRun().id());
        return problem;
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("Request failed unexpectedly", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR, "Internal error",
                "An unexpected error occurred.");
    }

    private static ProblemDetail problem(HttpStatus status, URI type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(title);
        return problem;
    }
}
