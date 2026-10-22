package cz.vojtechhubner.ruian.adapter.out.persistence;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import cz.vojtechhubner.ruian.adapter.out.persistence.entity.MunicipalityEntity;
import cz.vojtechhubner.ruian.adapter.out.persistence.entity.MunicipalityPartEntity;
import cz.vojtechhubner.ruian.adapter.out.persistence.repository.MunicipalityJpaRepository;
import cz.vojtechhubner.ruian.adapter.out.persistence.repository.MunicipalityPartJpaRepository;
import cz.vojtechhubner.ruian.domain.model.Municipality;
import cz.vojtechhubner.ruian.domain.model.MunicipalityDataset;
import cz.vojtechhubner.ruian.domain.model.MunicipalityPart;
import cz.vojtechhubner.ruian.domain.port.out.MunicipalityStore;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hibernate.Session;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA implementation of {@link MunicipalityStore}. {@link #replace} upserts a municipality and its parts in one
 * transaction under a PostgreSQL advisory lock; rows that did not change are left untouched, so their audit
 * timestamps only move when something about them changed.
 */
@Component
public class MunicipalityPersistenceAdapter implements MunicipalityStore {

    /** Distinguishes the municipality locks from any other advisory lock taken in the database. */
    private static final int LOCK_NAMESPACE = 1;

    private final MunicipalityJpaRepository municipalities;
    private final MunicipalityPartJpaRepository parts;
    private final EntityManager entityManager;

    public MunicipalityPersistenceAdapter(
            MunicipalityJpaRepository municipalities,
            MunicipalityPartJpaRepository parts,
            EntityManager entityManager) {
        this.municipalities = municipalities;
        this.parts = parts;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public int replace(MunicipalityDataset dataset) {
        lockMunicipality(dataset.municipality().code());
        upsertMunicipality(dataset.municipality());
        replaceParts(dataset);
        return dataset.partCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Municipality> findAll() {
        return municipalities.findAll(Sort.by("code")).stream().map(PersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Municipality> findByCode(int code) {
        return municipalities.findById(code).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MunicipalityPart> findPartsByMunicipalityCode(int municipalityCode) {
        return parts.findByMunicipalityCodeOrderByCode(municipalityCode).stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }

    /**
     * Takes a transaction-scoped advisory lock keyed by the municipality code. It serialises concurrent imports of
     * the same municipality across application instances and is released automatically at commit or rollback.
     */
    private void lockMunicipality(int code) {
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (var statement = connection.prepareStatement("select pg_advisory_xact_lock(?, ?)")) {
                statement.setInt(1, LOCK_NAMESPACE);
                statement.setInt(2, code);
                statement.execute();
            }
        });
    }

    /**
     * Renames the managed entity or persists a new one. A new entity is never passed to {@code save()}: with its
     * assigned id that would merge, and a merge copies the null {@code createdAt} over the audited value.
     */
    private void upsertMunicipality(Municipality municipality) {
        municipalities.findById(municipality.code()).ifPresentOrElse(
                entity -> entity.setName(municipality.name()),
                () -> entityManager.persist(new MunicipalityEntity(municipality.code(), municipality.name())));
    }

    /**
     * Parts are resolved by their own, nationally unique codes rather than through the municipality, so that a part
     * moved from another municipality between two snapshots is re-assigned instead of inserted a second time. Parts
     * of this municipality that are missing from the dataset are deleted.
     */
    private void replaceParts(MunicipalityDataset dataset) {
        int municipalityCode = dataset.municipality().code();
        Set<Integer> codes = dataset.parts().stream().map(MunicipalityPart::code).collect(toSet());
        var existing = parts.findAllById(codes).stream().collect(toMap(MunicipalityPartEntity::getCode, identity()));
        for (var part : dataset.parts()) {
            var entity = existing.get(part.code());
            if (entity == null) {
                entityManager.persist(new MunicipalityPartEntity(part.code(), part.name(), municipalityCode));
            } else {
                if (!entity.getName().equals(part.name())) {
                    entity.setName(part.name());
                }
                if (entity.getMunicipalityCode() != municipalityCode) {
                    entity.setMunicipalityCode(municipalityCode);
                }
            }
        }
        var stale = parts.findByMunicipalityCodeOrderByCode(municipalityCode).stream()
                .filter(entity -> !codes.contains(entity.getCode()))
                .toList();
        parts.deleteAll(stale);
    }
}
