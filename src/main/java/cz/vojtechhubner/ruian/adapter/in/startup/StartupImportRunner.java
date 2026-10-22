package cz.vojtechhubner.ruian.adapter.in.startup;

import cz.vojtechhubner.ruian.domain.exception.ImportException;
import cz.vojtechhubner.ruian.domain.model.ImportRun;
import cz.vojtechhubner.ruian.domain.port.in.ImportMunicipalityUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Runs the import of the configured default source once, after the application context is ready, when
 * {@code ruian.import.on-startup} is {@code true}. A failing import is recorded as a FAILED run by the service and
 * must never prevent the API from starting: the operator can retry it via {@code POST /api/imports}.
 */
@Component
@ConditionalOnProperty(prefix = "ruian.import", name = "on-startup", havingValue = "true")
public class StartupImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupImportRunner.class);

    private final ImportMunicipalityUseCase importUseCase;

    public StartupImportRunner(ImportMunicipalityUseCase importUseCase) {
        this.importUseCase = importUseCase;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Running the startup import of the configured default source");
        try {
            ImportRun run = importUseCase.importFrom(null);
            log.info("Startup import {} finished with status {}", run.id(), run.status());
        } catch (ImportException e) {
            // the message carries the reason; a failed download or parse is also recorded as a FAILED run
            log.warn("Startup import failed: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Startup import failed unexpectedly", e);
        }
    }
}
