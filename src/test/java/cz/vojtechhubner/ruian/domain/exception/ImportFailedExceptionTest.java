package cz.vojtechhubner.ruian.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import cz.vojtechhubner.ruian.domain.model.ImportRun;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ImportFailedExceptionTest {

    private static final URI SOURCE = URI.create("https://www.smartform.cz/download/kopidlno.xml.zip");
    private static final Instant STARTED_AT = Instant.parse("2026-10-22T10:00:00Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-10-22T10:00:05Z");

    @Test
    void messageNamesRunIdAndCause() {
        var failedRun = ImportRun.failed(SOURCE, STARTED_AT, FINISHED_AT, "unexpected element <Foo>").withId(7L);
        var cause = new VfrParseException("unexpected element <Foo>");

        var exception = new ImportFailedException(failedRun, cause);

        assertThat(exception).hasMessage("Import 7 failed: unexpected element <Foo>").hasCause(cause);
        assertThat(exception.failedRun()).isEqualTo(failedRun);
    }

    @Test
    void rejectsSucceededRun() {
        var succeededRun = ImportRun.succeeded(SOURCE, STARTED_AT, FINISHED_AT, 573060, 5).withId(7L);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ImportFailedException(succeededRun, new RuntimeException("boom")));
    }
}
