package cz.vojtechhubner.ruian.domain.model;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The record of one import attempt: where the data came from, when it ran and whether it succeeded. A SUCCEEDED run
 * carries the imported municipality and its part count, a FAILED run carries the error message instead.
 */
public record ImportRun(
        @Nullable Long id,
        URI sourceUrl,
        Instant startedAt,
        Instant finishedAt,
        ImportStatus status,
        @Nullable Integer municipalityCode,
        int partCount,
        @Nullable String errorMessage) {

    /** Error messages longer than this are truncated: a run keeps the gist of a failure, not a diagnostic dump. */
    public static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    public ImportRun {
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        Objects.requireNonNull(finishedAt, "finishedAt must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (errorMessage != null && errorMessage.length() > MAX_ERROR_MESSAGE_LENGTH) {
            errorMessage = errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
        }
        if (finishedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("finishedAt " + finishedAt + " is before startedAt " + startedAt);
        }
        if ((status == ImportStatus.FAILED) != (errorMessage != null)) {
            throw new IllegalArgumentException("errorMessage must be present exactly when status is FAILED");
        }
        if (status == ImportStatus.SUCCEEDED) {
            if (municipalityCode == null) {
                throw new IllegalArgumentException("a SUCCEEDED run must have a municipalityCode");
            }
            if (partCount < 0) {
                throw new IllegalArgumentException("partCount must not be negative but was " + partCount);
            }
        } else if (municipalityCode != null || partCount != 0) {
            throw new IllegalArgumentException("a FAILED run must have no municipalityCode and no parts");
        }
    }

    public static ImportRun succeeded(
            URI sourceUrl, Instant startedAt, Instant finishedAt, int municipalityCode, int partCount) {
        return new ImportRun(
                null, sourceUrl, startedAt, finishedAt, ImportStatus.SUCCEEDED, municipalityCode, partCount, null);
    }

    public static ImportRun failed(URI sourceUrl, Instant startedAt, Instant finishedAt, String errorMessage) {
        return new ImportRun(null, sourceUrl, startedAt, finishedAt, ImportStatus.FAILED, null, 0, errorMessage);
    }

    /** Returns a copy of this run with the given persistent id. */
    public ImportRun withId(long id) {
        return new ImportRun(id, sourceUrl, startedAt, finishedAt, status, municipalityCode, partCount, errorMessage);
    }
}
