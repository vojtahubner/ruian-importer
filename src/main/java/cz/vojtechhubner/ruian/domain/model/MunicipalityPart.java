package cz.vojtechhubner.ruian.domain.model;

import static cz.vojtechhubner.ruian.domain.model.Validation.requireName;
import static cz.vojtechhubner.ruian.domain.model.Validation.requirePositive;

/**
 * A part of a municipality (část obce) identified by its RÚIAN code, together with the code of the municipality
 * it belongs to.
 */
public record MunicipalityPart(int code, String name, int municipalityCode) {

    public MunicipalityPart {
        requirePositive(code, "code");
        requireName(name, "name");
        requirePositive(municipalityCode, "municipalityCode");
    }
}
