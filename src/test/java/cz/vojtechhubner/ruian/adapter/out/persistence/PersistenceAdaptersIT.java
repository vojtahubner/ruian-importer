package cz.vojtechhubner.ruian.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import cz.vojtechhubner.ruian.MutableClock;
import cz.vojtechhubner.ruian.TestClockConfiguration;
import cz.vojtechhubner.ruian.TestcontainersConfiguration;
import cz.vojtechhubner.ruian.adapter.out.persistence.repository.MunicipalityJpaRepository;
import cz.vojtechhubner.ruian.adapter.out.persistence.repository.MunicipalityPartJpaRepository;
import cz.vojtechhubner.ruian.config.JpaConfig;
import cz.vojtechhubner.ruian.domain.model.ImportRun;
import cz.vojtechhubner.ruian.domain.model.Municipality;
import cz.vojtechhubner.ruian.domain.model.MunicipalityDataset;
import cz.vojtechhubner.ruian.domain.model.MunicipalityPart;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs the persistence adapters against a real PostgreSQL: upserts, deletion of stale parts, audit timestamps and
 * import run round trips. Every adapter call that writes is followed by a flush and a clear of the persistence
 * context, so that the assertions read what is in the database.
 */
@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaConfig.class, TestClockConfiguration.class,
        MunicipalityPersistenceAdapter.class, ImportRunPersistenceAdapter.class})
@ActiveProfiles("test")
class PersistenceAdaptersIT {

    private static final int KOPIDLNO = 573060;
    private static final int JICIN = 573001;
    private static final URI SOURCE = URI.create("https://www.smartform.cz/download/kopidlno.xml.zip");
    private static final Instant STARTED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-01-01T10:00:05Z");

    @Autowired
    private MunicipalityPersistenceAdapter municipalityStore;

    @Autowired
    private ImportRunPersistenceAdapter importRunStore;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MutableClock clock;

    @Autowired
    private MunicipalityJpaRepository municipalities;

    @Autowired
    private MunicipalityPartJpaRepository municipalityParts;

    @Test
    void replaceStoresMunicipalityAndPartsOrderedByCode() {
        int stored = replaceAndFlush(kopidlno());

        assertThat(stored).isEqualTo(5);
        assertThat(municipalityStore.findAll()).containsExactly(new Municipality(KOPIDLNO, "Kopidlno"));
        assertThat(municipalityStore.findPartsByMunicipalityCode(KOPIDLNO)).containsExactly(
                new MunicipalityPart(31801, "Drahoraz", KOPIDLNO),
                new MunicipalityPart(31828, "Pševes", KOPIDLNO),
                new MunicipalityPart(69299, "Kopidlno", KOPIDLNO),
                new MunicipalityPart(69302, "Ledkov", KOPIDLNO),
                new MunicipalityPart(97373, "Mlýnec", KOPIDLNO));
        var municipality = municipalities.findById(KOPIDLNO).orElseThrow();
        assertThat(municipality.getCreatedAt()).isEqualTo(clock.instant());
        assertThat(municipality.getUpdatedAt()).isEqualTo(clock.instant());
    }

    @Test
    void replaceUpdatesChangedRowsAndDeletesPartsMissingFromTheDataset() {
        replaceAndFlush(kopidlno());
        var firstImport = clock.instant();
        clock.advance(Duration.ofHours(1));

        var changed = new MunicipalityDataset(new Municipality(KOPIDLNO, "Kopidlno"), List.of(
                new MunicipalityPart(69299, "Kopidlno", KOPIDLNO),
                new MunicipalityPart(69302, "Ledkov Nový", KOPIDLNO),
                new MunicipalityPart(97373, "Mlýnec", KOPIDLNO),
                new MunicipalityPart(31801, "Drahoraz", KOPIDLNO)));
        int stored = replaceAndFlush(changed);

        assertThat(stored).isEqualTo(4);
        assertThat(municipalityStore.findByCode(KOPIDLNO)).contains(new Municipality(KOPIDLNO, "Kopidlno"));
        assertThat(municipalityStore.findPartsByMunicipalityCode(KOPIDLNO)).containsExactly(
                new MunicipalityPart(31801, "Drahoraz", KOPIDLNO),
                new MunicipalityPart(69299, "Kopidlno", KOPIDLNO),
                new MunicipalityPart(69302, "Ledkov Nový", KOPIDLNO),
                new MunicipalityPart(97373, "Mlýnec", KOPIDLNO));
        assertThat(municipalityParts.findById(31828)).isEmpty();
        // Only changed rows are updated: the municipality (same name) and part 69299 keep their updated_at.
        var municipality = municipalities.findById(KOPIDLNO).orElseThrow();
        assertThat(municipality.getCreatedAt()).isEqualTo(firstImport);
        assertThat(municipality.getUpdatedAt()).isEqualTo(firstImport);
        assertThat(municipalityParts.findById(69302).orElseThrow().getUpdatedAt()).isEqualTo(clock.instant());
        assertThat(municipalityParts.findById(69299).orElseThrow().getUpdatedAt()).isEqualTo(firstImport);
    }

    @Test
    void replaceRenamesExistingMunicipalityAndAdvancesItsUpdatedAt() {
        replaceAndFlush(kopidlno());
        var firstImport = clock.instant();
        clock.advance(Duration.ofHours(1));

        var renamed = new MunicipalityDataset(new Municipality(KOPIDLNO, "Kopidlno u Jičína"), kopidlno().parts());
        replaceAndFlush(renamed);

        assertThat(municipalityStore.findByCode(KOPIDLNO)).contains(new Municipality(KOPIDLNO, "Kopidlno u Jičína"));
        var municipality = municipalities.findById(KOPIDLNO).orElseThrow();
        assertThat(municipality.getCreatedAt()).isEqualTo(firstImport);
        assertThat(municipality.getUpdatedAt()).isEqualTo(clock.instant());
    }

    @Test
    void replaceOfAnotherMunicipalityLeavesTheExistingOneIntact() {
        replaceAndFlush(kopidlno());

        replaceAndFlush(jicin(new MunicipalityPart(999001, "Staré Město", JICIN)));

        assertThat(municipalityStore.findAll())
                .containsExactly(new Municipality(JICIN, "Jičín"), new Municipality(KOPIDLNO, "Kopidlno"));
        assertThat(municipalityStore.findPartsByMunicipalityCode(KOPIDLNO))
                .extracting(MunicipalityPart::code)
                .containsExactly(31801, 31828, 69299, 69302, 97373);
        assertThat(municipalityStore.findPartsByMunicipalityCode(JICIN))
                .containsExactly(new MunicipalityPart(999001, "Staré Město", JICIN));
    }

    @Test
    void replaceMovesPartStoredUnderAnotherMunicipalityInsteadOfInsertingItAgain() {
        replaceAndFlush(jicin(new MunicipalityPart(31828, "Pševes", JICIN)));

        int stored = replaceAndFlush(kopidlno());

        assertThat(stored).isEqualTo(5);
        assertThat(municipalityParts.findById(31828).orElseThrow().getMunicipalityCode()).isEqualTo(KOPIDLNO);
        assertThat(municipalityStore.findPartsByMunicipalityCode(JICIN)).isEmpty();
    }

    @Test
    void findByCodeAndFindPartsAreEmptyForAnUnknownMunicipality() {
        assertThat(municipalityStore.findByCode(1)).isEmpty();
        assertThat(municipalityStore.findPartsByMunicipalityCode(1)).isEmpty();
    }

    @Test
    void saveReturnsSucceededRunWithGeneratedId() {
        var run = ImportRun.succeeded(SOURCE, STARTED_AT, FINISHED_AT, KOPIDLNO, 5);

        var saved = saveAndFlush(run);

        assertThat(saved.id()).isNotNull();
        assertThat(saved).isEqualTo(run.withId(saved.id()));
        assertThat(importRunStore.findById(saved.id())).contains(saved);
    }

    @Test
    void saveRoundTripsErrorMessageOfFailedRun() {
        var run = ImportRun.failed(SOURCE, STARTED_AT, FINISHED_AT, "connection refused");

        var saved = saveAndFlush(run);

        assertThat(saved.id()).isNotNull();
        assertThat(importRunStore.findById(saved.id())).contains(run.withId(saved.id()));
    }

    @Test
    void findLatestReturnsNewestRunsFirst() {
        var later = saveAndFlush(ImportRun.succeeded(
                SOURCE, STARTED_AT.plus(Duration.ofHours(1)), FINISHED_AT.plus(Duration.ofHours(1)), KOPIDLNO, 5));
        var earlier = saveAndFlush(ImportRun.failed(SOURCE, STARTED_AT, FINISHED_AT, "connection refused"));

        assertThat(importRunStore.findLatest(1)).containsExactly(later);
        assertThat(importRunStore.findLatest(10)).containsExactly(later, earlier);
    }

    @Test
    void findByIdIsEmptyForAnUnknownId() {
        assertThat(importRunStore.findById(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void findLatestRejectsLimitBelowOne() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> importRunStore.findLatest(0))
                .withMessageContaining("limit");
    }

    private int replaceAndFlush(MunicipalityDataset dataset) {
        int stored = municipalityStore.replace(dataset);
        entityManager.flush();
        entityManager.clear();
        return stored;
    }

    private ImportRun saveAndFlush(ImportRun run) {
        var saved = importRunStore.save(run);
        entityManager.flush();
        entityManager.clear();
        return saved;
    }

    private static MunicipalityDataset kopidlno() {
        return new MunicipalityDataset(new Municipality(KOPIDLNO, "Kopidlno"), List.of(
                new MunicipalityPart(69299, "Kopidlno", KOPIDLNO),
                new MunicipalityPart(69302, "Ledkov", KOPIDLNO),
                new MunicipalityPart(97373, "Mlýnec", KOPIDLNO),
                new MunicipalityPart(31801, "Drahoraz", KOPIDLNO),
                new MunicipalityPart(31828, "Pševes", KOPIDLNO)));
    }

    private static MunicipalityDataset jicin(MunicipalityPart part) {
        return new MunicipalityDataset(new Municipality(JICIN, "Jičín"), List.of(part));
    }
}
