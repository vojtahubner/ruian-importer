package cz.vojtechhubner.ruian.domain.model;

import org.jspecify.annotations.Nullable;

/**
 * Argument checks shared by the model records; every failure is an {@link IllegalArgumentException} naming the field.
 */
final class Validation {

    /** Maximum length of a municipality or municipality part name in RÚIAN. */
    private static final int MAX_NAME_LENGTH = 48;

    private Validation() {
    }

    static void requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive but was " + value);
        }
    }

    static void requireName(@Nullable String name, String field) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(field + " must be at most " + MAX_NAME_LENGTH
                    + " characters but was " + name.length());
        }
    }
}
