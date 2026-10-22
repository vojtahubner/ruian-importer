package cz.vojtechhubner.ruian.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ImportRunTest {

    private static final URI SOURCE = URI.create("https://www.smartform.cz/download/kopidlno.xml.zip");
    private static final Instant STARTED_AT = Instant.parse("2026-10-22T10:00:00Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-10-22T10:00:05Z");

    @Test
    void succeededFactoryProducesConsistentRun() {
        var run = ImportRun.succeeded(SOURCE, STARTED_AT, FINISHED_AT, 573060, 5);

        assertThat(run).isEqualTo(
                new ImportRun(null, SOURCE, STARTED_AT, FINISHED_AT, ImportStatus.SUCCEEDED, 573060, 5, null));
    }

    @Test
    void failedFactoryProducesConsistentRun() {
        var run = ImportRun.failed(SOURCE, STARTED_AT, FINISHED_AT, "connection refused");

        assertThat(run).isEqualTo(new ImportRun(
                null, SOURCE, STARTED_AT, FINISHED_AT, ImportStatus.FAILED, null, 0, "connection refused"));
    }

    @Test
    void withIdReturnsCopyWithIdSet() {
        var run = ImportRun.succeeded(SOURCE, STARTED_AT, FINISHED_AT, 573060, 5);

        var persisted = run.withId(42L);

        assertThat(persisted).isEqualTo(
                new ImportRun(42L, SOURCE, STARTED_AT, FINISHED_AT, ImportStatus.SUCCEEDED, 573060, 5, null));
        assertThat(run.id()).isNull();
    }

    @Test
    void rejectsFinishedAtBeforeStartedAt() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ImportRun.succeeded(SOURCE, FINISHED_AT, STARTED_AT, 573060, 5))
                .withMessageContaining("finishedAt");
    }

    @Test
    void rejectsSucceededRunWithErrorMessage() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ImportRun(
                null, SOURCE, STARTED_AT, FINISHED_AT, ImportStatus.SUCCEEDED, 573060, 5, "unexpected"));
    }

    @Test
    void rejectsFailedRunWithoutErrorMessage() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ImportRun(
                null, SOURCE, STARTED_AT, FINISHED_AT, ImportStatus.FAILED, null, 0, null));
    }

    @Test
    void rejectsFailedRunWithMunicipalityCode() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ImportRun(
                null, SOURCE, STARTED_AT, FINISHED_AT, ImportStatus.FAILED, 573060, 0, "connection refused"));
    }

    @Test
    void rejectsFailedRunWithParts() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ImportRun(
                null, SOURCE, STARTED_AT, FINISHED_AT, ImportStatus.FAILED, null, 1, "connection refused"));
    }

    @Test
    void rejectsSucceededRunWithoutMunicipalityCode() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ImportRun(
                null, SOURCE, STARTED_AT, FINISHED_AT, ImportStatus.SUCCEEDED, null, 5, null));
    }

    @Test
    void rejectsNegativePartCount() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ImportRun.succeeded(SOURCE, STARTED_AT, FINISHED_AT, 573060, -1))
                .withMessageContaining("partCount");
    }

    @Test
    void truncatesErrorMessageToMaxLength() {
        var tooLong = "x".repeat(ImportRun.MAX_ERROR_MESSAGE_LENGTH + 1);

        var run = ImportRun.failed(SOURCE, STARTED_AT, FINISHED_AT, tooLong);

        assertThat(run.errorMessage()).hasSize(1000).isEqualTo(tooLong.substring(0, 1000));
    }
}
