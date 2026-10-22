package cz.vojtechhubner.ruian.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import cz.vojtechhubner.ruian.domain.model.ImportRun;
import cz.vojtechhubner.ruian.domain.model.ImportStatus;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ImportRunResponseTest {

    private static final URI SOURCE = URI.create("https://www.smartform.cz/download/kopidlno.xml.zip");
    private static final Instant STARTED_AT = Instant.parse("2026-10-22T10:00:00Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-10-22T10:00:05Z");

    @Test
    void mapsAPersistedRun() {
        var run = ImportRun.failed(SOURCE, STARTED_AT, FINISHED_AT, "unexpected element <Foo>").withId(7L);

        assertThat(ImportRunResponse.from(run)).isEqualTo(new ImportRunResponse(
                7L, SOURCE.toString(), ImportStatus.FAILED, STARTED_AT, FINISHED_AT, null, 0,
                "unexpected element <Foo>"));
    }

    @Test
    void rejectsARunWithoutId() {
        var run = ImportRun.succeeded(SOURCE, STARTED_AT, FINISHED_AT, 573060, 5);

        assertThatNullPointerException()
                .isThrownBy(() -> ImportRunResponse.from(run))
                .withMessage("run must have an id");
    }
}
