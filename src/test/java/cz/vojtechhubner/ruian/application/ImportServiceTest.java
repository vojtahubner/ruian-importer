package cz.vojtechhubner.ruian.application;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cz.vojtechhubner.ruian.MutableClock;
import cz.vojtechhubner.ruian.domain.exception.ImportFailedException;
import cz.vojtechhubner.ruian.domain.exception.NotFoundException;
import cz.vojtechhubner.ruian.domain.exception.SourceNotAllowedException;
import cz.vojtechhubner.ruian.domain.exception.VfrParseException;
import cz.vojtechhubner.ruian.domain.model.ImportRun;
import cz.vojtechhubner.ruian.domain.model.Municipality;
import cz.vojtechhubner.ruian.domain.model.MunicipalityDataset;
import cz.vojtechhubner.ruian.domain.model.MunicipalityPart;
import cz.vojtechhubner.ruian.domain.port.out.ImportRunStore;
import cz.vojtechhubner.ruian.domain.port.out.MunicipalityStore;
import cz.vojtechhubner.ruian.domain.port.out.VfrParser;
import cz.vojtechhubner.ruian.domain.port.out.VfrSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    private static final URI DEFAULT_SOURCE = URI.create("https://www.smartform.cz/download/kopidlno.xml.zip");
    private static final URI SOURCE = URI.create("https://vdp.cuzk.gov.cz/kopidlno.xml.zip");
    private static final Instant STARTED_AT = Instant.parse("2026-10-22T10:00:00Z");
    private static final Duration IMPORT_DURATION = Duration.ofSeconds(5);
    private static final int KOPIDLNO = 573060;
    private static final MunicipalityDataset DATASET = new MunicipalityDataset(
            new Municipality(KOPIDLNO, "Kopidlno"),
            List.of(
                    new MunicipalityPart(69299, "Kopidlno", KOPIDLNO),
                    new MunicipalityPart(69302, "Ledkov", KOPIDLNO)));
    private static final ImportProperties PROPERTIES = new ImportProperties(
            DEFAULT_SOURCE, List.of("smartform.cz", "vdp.cuzk.gov.cz"), false, DataSize.ofMegabytes(64));

    private final FakeVfrSource source = new FakeVfrSource();
    private final MutableClock clock = new MutableClock(STARTED_AT);

    @Mock
    private VfrParser parser;

    @Mock
    private MunicipalityStore municipalities;

    @Mock
    private ImportRunStore runs;

    private ImportService service;

    @BeforeEach
    void setUp() {
        service = new ImportService(
                PROPERTIES, new SourceUrlPolicy(PROPERTIES), source, parser, municipalities, runs, clock);
    }

    /** The parser yields the dataset five seconds in, the store reports two parts and the saved run gets id 42. */
    private void stubSuccessfulImport() {
        when(parser.parse(any())).thenAnswer(invocation -> {
            clock.advance(IMPORT_DURATION);
            return DATASET;
        });
        when(municipalities.replace(DATASET)).thenReturn(2);
        stubSaveAssigningId(42L);
    }

    private void stubSaveAssigningId(long id) {
        when(runs.save(any())).thenAnswer(invocation -> invocation.<ImportRun>getArgument(0).withId(id));
    }

    @Test
    void importsFromTheRequestedSourceAndRecordsTheSucceededRun() {
        stubSuccessfulImport();

        var run = service.importFrom(SOURCE);

        assertThat(source.requested).isEqualTo(SOURCE);
        var xml = ArgumentCaptor.forClass(InputStream.class);
        verify(parser).parse(xml.capture());
        assertThat(xml.getValue()).hasContent(FakeVfrSource.XML);
        verify(municipalities).replace(DATASET);
        assertThat(run).isEqualTo(
                ImportRun.succeeded(SOURCE, STARTED_AT, STARTED_AT.plus(IMPORT_DURATION), KOPIDLNO, 2).withId(42L));
    }

    @Test
    void importsFromTheDefaultSourceWhenNoneIsRequested() {
        stubSuccessfulImport();

        var run = service.importFrom(null);

        assertThat(source.requested).isEqualTo(DEFAULT_SOURCE);
        assertThat(run.sourceUrl()).isEqualTo(DEFAULT_SOURCE);
    }

    @Test
    void checksThePolicyBeforeTouchingSourceParserOrStores() {
        assertThatExceptionOfType(SourceNotAllowedException.class)
                .isThrownBy(() -> service.importFrom(URI.create("http://www.smartform.cz/x.zip")))
                .withMessageContaining("only https is allowed");

        assertThat(source.requested).isNull();
        verifyNoInteractions(parser, municipalities, runs);
    }

    @Test
    void recordsAFailedRunWhenParsingFails() {
        var failure = new VfrParseException("bad vfr");
        when(parser.parse(any())).thenThrow(failure);
        stubSaveAssigningId(7L);

        assertThatExceptionOfType(ImportFailedException.class)
                .isThrownBy(() -> service.importFrom(SOURCE))
                .satisfies(e -> {
                    assertThat(e.getCause()).isSameAs(failure);
                    assertThat(e.failedRun())
                            .isEqualTo(ImportRun.failed(SOURCE, STARTED_AT, STARTED_AT, "bad vfr").withId(7L));
                });
        verifyNoInteractions(municipalities);
    }

    @Test
    void hidesUnexpectedFailureDetailsFromTheRecordedRun() {
        when(parser.parse(any())).thenReturn(DATASET);
        when(municipalities.replace(DATASET)).thenThrow(new RuntimeException("secret sql"));
        stubSaveAssigningId(7L);

        assertThatExceptionOfType(ImportFailedException.class)
                .isThrownBy(() -> service.importFrom(SOURCE))
                .satisfies(e -> assertThat(e.failedRun().errorMessage())
                        .isEqualTo("Unexpected RuntimeException (see server log)")
                        .doesNotContain("secret sql"));
    }

    @Test
    void latestDelegatesToTheStore() {
        var run = ImportRun.succeeded(SOURCE, STARTED_AT, STARTED_AT, KOPIDLNO, 2).withId(42L);
        when(runs.findLatest(5)).thenReturn(List.of(run));

        assertThat(service.latest(5)).containsExactly(run);
    }

    @Test
    void byIdReturnsTheStoredRun() {
        var run = ImportRun.succeeded(SOURCE, STARTED_AT, STARTED_AT, KOPIDLNO, 2).withId(42L);
        when(runs.findById(42L)).thenReturn(Optional.of(run));

        assertThat(service.byId(42L)).isEqualTo(run);
    }

    @Test
    void byIdOfUnknownRunIsNotFound() {
        when(runs.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> service.byId(99L))
                .withMessage("Import run 99 not found")
                .satisfies(e -> {
                    assertThat(e.resource()).isEqualTo("Import run");
                    assertThat(e.id()).isEqualTo(99L);
                });
    }

    /** Hands the reader an in-memory stream, so that the parser is really invoked through the execute-around port. */
    private static final class FakeVfrSource implements VfrSource {

        static final String XML = "<vf:VymennyFormat/>";

        private @Nullable URI requested;

        @Override
        public <T> T read(URI source, Reader<T> reader) {
            requested = source;
            try {
                return reader.read(new ByteArrayInputStream(XML.getBytes(UTF_8)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
