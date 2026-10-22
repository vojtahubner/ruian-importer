package cz.vojtechhubner.ruian.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import cz.vojtechhubner.ruian.MutableClock;
import cz.vojtechhubner.ruian.TestClockConfiguration;
import cz.vojtechhubner.ruian.TestcontainersConfiguration;
import cz.vojtechhubner.ruian.adapter.out.persistence.entity.ImportRunEntity;
import cz.vojtechhubner.ruian.adapter.out.persistence.entity.MunicipalityEntity;
import cz.vojtechhubner.ruian.adapter.out.persistence.entity.MunicipalityPartEntity;
import cz.vojtechhubner.ruian.adapter.out.persistence.repository.ImportRunJpaRepository;
import cz.vojtechhubner.ruian.adapter.out.persistence.repository.MunicipalityJpaRepository;
import cz.vojtechhubner.ruian.adapter.out.persistence.repository.MunicipalityPartJpaRepository;
import cz.vojtechhubner.ruian.config.JpaConfig;
import cz.vojtechhubner.ruian.domain.model.ImportStatus;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Checks the JPA mappings against the real schema: the context only starts if Hibernate's schema validation accepts
 * the Flyway-created tables.
 */
@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaConfig.class, TestClockConfiguration.class})
@ActiveProfiles("test")
class JpaMappingIT {

    private static final int KOPIDLNO = 573060;
    private static final String SOURCE_URL = "https://www.smartform.cz/download/kopidlno.xml.zip";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MunicipalityJpaRepository municipalities;

    @Autowired
    private MunicipalityPartJpaRepository municipalityParts;

    @Autowired
    private ImportRunJpaRepository importRuns;

    @Autowired
    private MutableClock clock;

    @Test
    void flywayCreatedTheTables() {
        var tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = current_schema()",
                String.class);

        assertThat(tables).contains("municipality", "municipality_part", "import_run");
    }

    @Test
    void auditTimestampsComeFromTheApplicationClock() {
        var createdAt = clock.instant();
        entityManager.persistAndFlush(new MunicipalityEntity(KOPIDLNO, "Kopidlno"));
        entityManager.clear();

        var created = municipalities.findById(KOPIDLNO).orElseThrow();
        assertThat(created.getCreatedAt()).isEqualTo(createdAt);
        assertThat(created.getUpdatedAt()).isEqualTo(createdAt);

        clock.advance(Duration.ofHours(1));
        created.setName("Kopidlno u Jičína");
        entityManager.flush();
        entityManager.clear();

        var updated = municipalities.findById(KOPIDLNO).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Kopidlno u Jičína");
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isEqualTo(createdAt.plus(Duration.ofHours(1)));
    }

    @Test
    void deletingAMunicipalityCascadesToItsPartsInTheDatabase() {
        entityManager.persist(new MunicipalityEntity(KOPIDLNO, "Kopidlno"));
        entityManager.persist(new MunicipalityPartEntity(409979, "Kopidlno", KOPIDLNO));
        entityManager.persist(new MunicipalityPartEntity(409987, "Pševes", KOPIDLNO));
        entityManager.flush();

        jdbcTemplate.update("delete from municipality where code = ?", KOPIDLNO);
        entityManager.clear();

        assertThat(municipalities.findById(KOPIDLNO)).isEmpty();
        assertThat(municipalityParts.findByMunicipalityCodeOrderByCode(KOPIDLNO)).isEmpty();
    }

    @Test
    void importRunRoundTripsWithGeneratedIdAndNewestComesFirst() {
        var failed = new ImportRunEntity(SOURCE_URL, Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:05Z"), ImportStatus.FAILED, null, 0, "connection refused");
        var succeeded = new ImportRunEntity(SOURCE_URL, Instant.parse("2026-01-01T11:00:00Z"),
                Instant.parse("2026-01-01T11:00:05Z"), ImportStatus.SUCCEEDED, KOPIDLNO, 5, null);
        entityManager.persist(failed);
        entityManager.persist(succeeded);
        entityManager.flush();
        entityManager.clear();

        assertThat(failed.getId()).isNotNull();
        assertThat(succeeded.getId()).isNotNull();
        var newest = importRuns.findByOrderByStartedAtDescIdDesc(Limit.of(1));
        assertThat(newest).hasSize(1);
        var reloaded = newest.getFirst();
        assertThat(reloaded.getId()).isEqualTo(succeeded.getId());
        assertThat(reloaded.getSourceUrl()).isEqualTo(SOURCE_URL);
        assertThat(reloaded.getStartedAt()).isEqualTo(Instant.parse("2026-01-01T11:00:00Z"));
        assertThat(reloaded.getFinishedAt()).isEqualTo(Instant.parse("2026-01-01T11:00:05Z"));
        assertThat(reloaded.getStatus()).isEqualTo(ImportStatus.SUCCEEDED);
        assertThat(reloaded.getMunicipalityCode()).isEqualTo(KOPIDLNO);
        assertThat(reloaded.getPartCount()).isEqualTo(5);
        assertThat(reloaded.getErrorMessage()).isNull();
    }
}
