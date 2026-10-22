package cz.vojtechhubner.ruian.application;

import cz.vojtechhubner.ruian.domain.exception.ImportException;
import cz.vojtechhubner.ruian.domain.exception.ImportFailedException;
import cz.vojtechhubner.ruian.domain.exception.NotFoundException;
import cz.vojtechhubner.ruian.domain.model.ImportRun;
import cz.vojtechhubner.ruian.domain.model.MunicipalityDataset;
import cz.vojtechhubner.ruian.domain.port.in.ImportMunicipalityUseCase;
import cz.vojtechhubner.ruian.domain.port.in.ImportRunQuery;
import cz.vojtechhubner.ruian.domain.port.out.ImportRunStore;
import cz.vojtechhubner.ruian.domain.port.out.MunicipalityStore;
import cz.vojtechhubner.ruian.domain.port.out.VfrParser;
import cz.vojtechhubner.ruian.domain.port.out.VfrSource;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Runs an import end to end: checks the source against the policy, streams it through the parser into the store
 * and records the outcome as an {@link ImportRun}; also answers the import run queries. Whatever fails after the
 * policy check is recorded as a FAILED run and rethrown as an {@link ImportFailedException}; the stores own the
 * transactions, so a failed import leaves the previously stored municipality untouched.
 */
@Service
public class ImportService implements ImportMunicipalityUseCase, ImportRunQuery {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final ImportProperties properties;
    private final SourceUrlPolicy policy;
    private final VfrSource source;
    private final VfrParser parser;
    private final MunicipalityStore municipalities;
    private final ImportRunStore runs;
    private final Clock clock;

    public ImportService(
            ImportProperties properties,
            SourceUrlPolicy policy,
            VfrSource source,
            VfrParser parser,
            MunicipalityStore municipalities,
            ImportRunStore runs,
            Clock clock) {
        this.properties = properties;
        this.policy = policy;
        this.source = source;
        this.parser = parser;
        this.municipalities = municipalities;
        this.runs = runs;
        this.clock = clock;
    }

    @Override
    public ImportRun importFrom(@Nullable URI requestedSource) {
        URI source = requestedSource != null ? requestedSource : properties.defaultSourceUrl();
        policy.ensureAllowed(source);
        Instant started = clock.instant();
        log.info("Import started from {}", source);
        try {
            MunicipalityDataset dataset = this.source.read(source, parser::parse);
            int partCount = municipalities.replace(dataset);
            ImportRun run = runs.save(
                    ImportRun.succeeded(source, started, clock.instant(), dataset.municipality().code(), partCount));
            log.info("Import {} succeeded: municipality {} ({}) with {} parts",
                    run.id(), dataset.municipality().name(), dataset.municipality().code(), partCount);
            return run;
        } catch (RuntimeException e) {
            ImportRun failed = runs.save(
                    ImportRun.failed(source, started, clock.instant(), ErrorMessages.sanitize(e)));
            if (e instanceof ImportException) {
                log.warn("Import {} failed: {}", failed.id(), e.getMessage());
            } else {
                log.error("Import {} failed unexpectedly", failed.id(), e);
            }
            throw new ImportFailedException(failed, e);
        }
    }

    @Override
    public List<ImportRun> latest(int limit) {
        return runs.findLatest(limit);
    }

    @Override
    public ImportRun byId(long id) {
        return runs.findById(id).orElseThrow(() -> new NotFoundException("Import run", id));
    }
}
