package cz.vojtechhubner.ruian.domain.port.in;

import cz.vojtechhubner.ruian.domain.exception.NotFoundException;
import cz.vojtechhubner.ruian.domain.model.ImportRun;
import java.util.List;

/**
 * Read access to the recorded import runs.
 */
public interface ImportRunQuery {

    /** Returns at most {@code limit} runs, most recent first. */
    List<ImportRun> latest(int limit);

    /**
     * @throws NotFoundException if no run has the given id
     */
    ImportRun byId(long id);
}
