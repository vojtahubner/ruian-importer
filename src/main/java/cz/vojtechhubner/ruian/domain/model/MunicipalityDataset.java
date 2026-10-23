package cz.vojtechhubner.ruian.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * A municipality together with all of its parts, as read from one VFR file.
 */
public record MunicipalityDataset(Municipality municipality, List<MunicipalityPart> parts) {

    public MunicipalityDataset {
        Objects.requireNonNull(municipality, "municipality must not be null");
        parts = List.copyOf(parts);
        var codes = new HashSet<Integer>();
        for (var part : parts) {
            if (part.municipalityCode() != municipality.code()) {
                throw new IllegalArgumentException("Part " + part.code() + " belongs to municipality "
                        + part.municipalityCode() + ", expected " + municipality.code());
            }
            if (!codes.add(part.code())) {
                throw new IllegalArgumentException("Duplicate part code " + part.code());
            }
        }
    }

    public int partCount() {
        return parts.size();
    }
}
