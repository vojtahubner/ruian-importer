package cz.vojtechhubner.ruian.application;

import static org.assertj.core.api.Assertions.assertThat;

import cz.vojtechhubner.ruian.domain.exception.ImportException;
import cz.vojtechhubner.ruian.domain.exception.VfrParseException;
import org.junit.jupiter.api.Test;

class ErrorMessagesTest {

    @Test
    void passesImportExceptionMessageThrough() {
        assertThat(ErrorMessages.sanitize(new VfrParseException("Malformed VFR XML: unexpected end")))
                .isEqualTo("Malformed VFR XML: unexpected end");
    }

    @Test
    void hidesOtherExceptionsBehindTheirType() {
        assertThat(ErrorMessages.sanitize(new RuntimeException("secret sql")))
                .isEqualTo("Unexpected RuntimeException (see server log)");
    }

    @Test
    void fallsBackToTheTypeWhenAnImportExceptionHasNoMessage() {
        assertThat(ErrorMessages.sanitize(new ImportException(null))).isEqualTo("ImportException");
    }
}
