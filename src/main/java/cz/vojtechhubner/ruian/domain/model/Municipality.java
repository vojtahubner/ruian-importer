package cz.vojtechhubner.ruian.domain.model;

import static cz.vojtechhubner.ruian.domain.model.Validation.requireName;
import static cz.vojtechhubner.ruian.domain.model.Validation.requirePositive;

/**
 * A municipality (obec) identified by its RÚIAN code.
 */
public record Municipality(int code, String name) {

    public Municipality {
        requirePositive(code, "code");
        requireName(name, "name");
    }
}
