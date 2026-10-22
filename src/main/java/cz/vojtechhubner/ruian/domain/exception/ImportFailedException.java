package cz.vojtechhubner.ruian.domain.exception;

import cz.vojtechhubner.ruian.domain.model.ImportRun;
import cz.vojtechhubner.ruian.domain.model.ImportStatus;

/**
 * An import ended with a FAILED run that has already been recorded; carries that run and the failure that caused it.
 */
public class ImportFailedException extends ImportException {

    private final ImportRun failedRun;

    public ImportFailedException(ImportRun failedRun, RuntimeException cause) {
        super("Import " + failedRun.id() + " failed: " + cause.getMessage(), cause);
        if (failedRun.status() != ImportStatus.FAILED) {
            throw new IllegalArgumentException("failedRun must have status FAILED but was " + failedRun.status());
        }
        this.failedRun = failedRun;
    }

    public ImportRun failedRun() {
        return failedRun;
    }
}
