package cz.vojtechhubner.ruian.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import cz.vojtechhubner.ruian.adapter.out.persistence.entity.ImportRunEntity;
import cz.vojtechhubner.ruian.adapter.out.persistence.entity.MunicipalityEntity;
import cz.vojtechhubner.ruian.adapter.out.persistence.entity.MunicipalityPartEntity;
import cz.vojtechhubner.ruian.domain.model.ImportRun;
import cz.vojtechhubner.ruian.domain.model.ImportStatus;
import cz.vojtechhubner.ruian.domain.model.Municipality;
import cz.vojtechhubner.ruian.domain.model.MunicipalityPart;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PersistenceMapperTest {

    private static final URI SOURCE = URI.create("https://www.smartform.cz/download/kopidlno.xml.zip");
    private static final Instant STARTED_AT = Instant.parse("2026-10-22T10:00:00Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-10-22T10:00:05Z");

    @Test
    void mapsMunicipalityEntityToRecord() {
        var municipality = PersistenceMapper.toDomain(new MunicipalityEntity(573060, "Kopidlno"));

        assertThat(municipality).isEqualTo(new Municipality(573060, "Kopidlno"));
    }

    @Test
    void mapsMunicipalityPartEntityToRecord() {
        var part = PersistenceMapper.toDomain(new MunicipalityPartEntity(69302, "Ledkov", 573060));

        assertThat(part).isEqualTo(new MunicipalityPart(69302, "Ledkov", 573060));
    }

    @Test
    void mapsSucceededRunToNewEntity() {
        var entity = PersistenceMapper.toEntity(ImportRun.succeeded(SOURCE, STARTED_AT, FINISHED_AT, 573060, 5));

        assertThat(entity.getId()).isNull();
        assertThat(entity.getSourceUrl()).isEqualTo(SOURCE.toString());
        assertThat(entity.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(entity.getFinishedAt()).isEqualTo(FINISHED_AT);
        assertThat(entity.getStatus()).isEqualTo(ImportStatus.SUCCEEDED);
        assertThat(entity.getMunicipalityCode()).isEqualTo(573060);
        assertThat(entity.getPartCount()).isEqualTo(5);
        assertThat(entity.getErrorMessage()).isNull();
    }

    @Test
    void mapsSucceededEntityToRunWithItsId() {
        var entity = new ImportRunEntity(
                SOURCE.toString(), STARTED_AT, FINISHED_AT, ImportStatus.SUCCEEDED, 573060, 5, null);
        ReflectionTestUtils.setField(entity, "id", 42L);

        var run = PersistenceMapper.toDomain(entity);

        assertThat(run).isEqualTo(ImportRun.succeeded(SOURCE, STARTED_AT, FINISHED_AT, 573060, 5).withId(42L));
    }

    @Test
    void mapsFailedRunToNewEntity() {
        var entity = PersistenceMapper.toEntity(
                ImportRun.failed(SOURCE, STARTED_AT, FINISHED_AT, "connection refused"));

        assertThat(entity.getId()).isNull();
        assertThat(entity.getSourceUrl()).isEqualTo(SOURCE.toString());
        assertThat(entity.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(entity.getFinishedAt()).isEqualTo(FINISHED_AT);
        assertThat(entity.getStatus()).isEqualTo(ImportStatus.FAILED);
        assertThat(entity.getMunicipalityCode()).isNull();
        assertThat(entity.getPartCount()).isZero();
        assertThat(entity.getErrorMessage()).isEqualTo("connection refused");
    }

    @Test
    void mapsFailedEntityToRun() {
        var entity = new ImportRunEntity(
                SOURCE.toString(), STARTED_AT, FINISHED_AT, ImportStatus.FAILED, null, 0, "connection refused");

        var run = PersistenceMapper.toDomain(entity);

        assertThat(run).isEqualTo(ImportRun.failed(SOURCE, STARTED_AT, FINISHED_AT, "connection refused"));
    }
}
