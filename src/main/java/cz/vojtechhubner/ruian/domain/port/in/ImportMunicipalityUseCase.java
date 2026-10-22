package cz.vojtechhubner.ruian.domain.port.in;

import cz.vojtechhubner.ruian.domain.exception.ImportFailedException;
import cz.vojtechhubner.ruian.domain.exception.SourceNotAllowedException;
import cz.vojtechhubner.ruian.domain.model.ImportRun;
import java.net.URI;
import org.jspecify.annotations.Nullable;

/**
 * Imports one municipality from a VFR source and records the outcome as an {@link ImportRun}.
 */
public interface ImportMunicipalityUseCase {

    /**
     * Downloads, parses and stores the municipality found at {@code sourceUrl}.
     *
     * @param sourceUrl the VFR archive to import, or {@code null} for the configured default source
     * @return the recorded SUCCEEDED run
     * @throws SourceNotAllowedException if the source is rejected by the import policy; no run is recorded
     * @throws ImportFailedException if downloading, parsing or storing fails; a FAILED run was recorded and is
     *         carried by the exception
     */
    ImportRun importFrom(@Nullable URI sourceUrl);
}
