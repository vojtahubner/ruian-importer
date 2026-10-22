package cz.vojtechhubner.ruian.adapter.in.startup;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cz.vojtechhubner.ruian.domain.exception.ImportFailedException;
import cz.vojtechhubner.ruian.domain.exception.SourceNotAllowedException;
import cz.vojtechhubner.ruian.domain.exception.VfrParseException;
import cz.vojtechhubner.ruian.domain.model.ImportRun;
import cz.vojtechhubner.ruian.domain.port.in.ImportMunicipalityUseCase;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class StartupImportRunnerTest {

    private static final URI SOURCE = URI.create("https://www.smartform.cz/download/kopidlno.xml.zip");
    private static final Instant STARTED_AT = Instant.parse("2026-10-22T10:00:00Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-10-22T10:00:05Z");
    private static final ApplicationArguments NO_ARGS = new DefaultApplicationArguments();

    @Mock
    private ImportMunicipalityUseCase importUseCase;

    private StartupImportRunner runner;

    @BeforeEach
    void setUp() {
        runner = new StartupImportRunner(importUseCase);
    }

    @Test
    void importsTheDefaultSourceOnce() {
        when(importUseCase.importFrom(null))
                .thenReturn(ImportRun.succeeded(SOURCE, STARTED_AT, FINISHED_AT, 573060, 5).withId(1));

        assertThatCode(() -> runner.run(NO_ARGS)).doesNotThrowAnyException();

        verify(importUseCase).importFrom(null);
    }

    @Test
    void swallowsAFailedImport() {
        var failedRun = ImportRun.failed(SOURCE, STARTED_AT, FINISHED_AT, "bad vfr").withId(1);
        when(importUseCase.importFrom(null))
                .thenThrow(new ImportFailedException(failedRun, new VfrParseException("bad vfr")));

        assertThatCode(() -> runner.run(NO_ARGS)).doesNotThrowAnyException();

        verify(importUseCase).importFrom(null);
    }

    @Test
    void swallowsARejectedSource() {
        when(importUseCase.importFrom(null))
                .thenThrow(new SourceNotAllowedException(SOURCE, "host www.smartform.cz is not on the allowlist"));

        assertThatCode(() -> runner.run(NO_ARGS)).doesNotThrowAnyException();

        verify(importUseCase).importFrom(null);
    }

    @Test
    void swallowsAnUnexpectedFailure() {
        when(importUseCase.importFrom(null)).thenThrow(new IllegalStateException("boom"));

        assertThatCode(() -> runner.run(NO_ARGS)).doesNotThrowAnyException();

        verify(importUseCase).importFrom(null);
    }
}
