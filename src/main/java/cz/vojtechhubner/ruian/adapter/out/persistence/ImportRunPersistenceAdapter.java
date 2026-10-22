package cz.vojtechhubner.ruian.adapter.out.persistence;

import cz.vojtechhubner.ruian.adapter.out.persistence.repository.ImportRunJpaRepository;
import cz.vojtechhubner.ruian.domain.model.ImportRun;
import cz.vojtechhubner.ruian.domain.port.out.ImportRunStore;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA implementation of {@link ImportRunStore}: inserts finished runs and reads them back, newest first.
 */
@Component
public class ImportRunPersistenceAdapter implements ImportRunStore {

    private final ImportRunJpaRepository runs;

    public ImportRunPersistenceAdapter(ImportRunJpaRepository runs) {
        this.runs = runs;
    }

    @Override
    @Transactional
    public ImportRun save(ImportRun run) {
        return PersistenceMapper.toDomain(runs.save(PersistenceMapper.toEntity(run)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImportRun> findById(long id) {
        return runs.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportRun> findLatest(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive but was " + limit);
        }
        return runs.findByOrderByStartedAtDescIdDesc(Limit.of(limit)).stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }
}
