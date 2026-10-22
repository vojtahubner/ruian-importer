package cz.vojtechhubner.ruian.adapter.out.persistence;

import cz.vojtechhubner.ruian.adapter.out.persistence.entity.ImportRunEntity;
import cz.vojtechhubner.ruian.adapter.out.persistence.entity.MunicipalityEntity;
import cz.vojtechhubner.ruian.adapter.out.persistence.entity.MunicipalityPartEntity;
import cz.vojtechhubner.ruian.domain.model.ImportRun;
import cz.vojtechhubner.ruian.domain.model.Municipality;
import cz.vojtechhubner.ruian.domain.model.MunicipalityPart;
import java.net.URI;

/**
 * Converts between the JPA entities and the domain records, so that no entity leaves the persistence adapter.
 */
final class PersistenceMapper {

    private PersistenceMapper() {
    }

    static Municipality toDomain(MunicipalityEntity entity) {
        return new Municipality(entity.getCode(), entity.getName());
    }

    static MunicipalityPart toDomain(MunicipalityPartEntity entity) {
        return new MunicipalityPart(entity.getCode(), entity.getName(), entity.getMunicipalityCode());
    }

    static ImportRun toDomain(ImportRunEntity entity) {
        return new ImportRun(
                entity.getId(),
                URI.create(entity.getSourceUrl()),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getStatus(),
                entity.getMunicipalityCode(),
                entity.getPartCount(),
                entity.getErrorMessage());
    }

    /** Builds a new, not yet persisted entity: its id stays null until the database generates one on insert. */
    static ImportRunEntity toEntity(ImportRun run) {
        return new ImportRunEntity(
                run.sourceUrl().toString(),
                run.startedAt(),
                run.finishedAt(),
                run.status(),
                run.municipalityCode(),
                run.partCount(),
                run.errorMessage());
    }
}
