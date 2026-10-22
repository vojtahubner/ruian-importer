package cz.vojtechhubner.ruian.domain.port.out;

import cz.vojtechhubner.ruian.domain.exception.VfrParseException;
import cz.vojtechhubner.ruian.domain.model.MunicipalityDataset;
import java.io.InputStream;

/**
 * Parses a VFR XML stream into the municipality it describes and that municipality's parts.
 */
public interface VfrParser {

    /**
     * @throws VfrParseException if the stream does not hold a valid VFR document
     */
    MunicipalityDataset parse(InputStream in);
}
