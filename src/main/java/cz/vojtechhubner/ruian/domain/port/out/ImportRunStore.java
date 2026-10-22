package cz.vojtechhubner.ruian.domain.port.out;

import cz.vojtechhubner.ruian.domain.model.ImportRun;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of import runs.
 */
public interface ImportRunStore {

    /** Persists the run and returns it with its id set. */
    ImportRun save(ImportRun run);

    Optional<ImportRun> findById(long id);

    /** Returns at most {@code limit} runs, most recent first. */
    List<ImportRun> findLatest(int limit);
}
