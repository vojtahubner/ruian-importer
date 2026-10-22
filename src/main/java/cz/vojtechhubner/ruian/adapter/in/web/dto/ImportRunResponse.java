package cz.vojtechhubner.ruian.adapter.in.web.dto;

import cz.vojtechhubner.ruian.domain.model.ImportRun;
import cz.vojtechhubner.ruian.domain.model.ImportStatus;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A recorded import run as exposed by the API.
 */
public record ImportRunResponse(
        long id,
        String sourceUrl,
        ImportStatus status,
        Instant startedAt,
        Instant finishedAt,
        @Nullable Integer municipalityCode,
        int partCount,
        @Nullable String errorMessage) {

    /** Maps a persisted run; a run without an id has not been recorded and cannot be exposed. */
    public static ImportRunResponse from(ImportRun run) {
        return new ImportRunResponse(
                Objects.requireNonNull(run.id(), "run must have an id"),
                run.sourceUrl().toString(),
                run.status(),
                run.startedAt(),
                run.finishedAt(),
                run.municipalityCode(),
                run.partCount(),
                run.errorMessage());
    }
}
