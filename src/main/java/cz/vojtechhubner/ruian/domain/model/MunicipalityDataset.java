package cz.vojtechhubner.ruian.domain.model;

import java.util.List;

/**
 * A municipality together with all of its parts, as read from one VFR file.
 */
public record MunicipalityDataset(Municipality municipality, List<MunicipalityPart> parts) {

    public MunicipalityDataset {
        parts = List.copyOf(parts);
        for (var part : parts) {
            if (part.municipalityCode() != municipality.code()) {
                throw new IllegalArgumentException("part " + part.code() + " belongs to municipality "
                        + part.municipalityCode() + ", not to " + municipality.code());
            }
        }
    }

    public int partCount() {
        return parts.size();
    }
}
