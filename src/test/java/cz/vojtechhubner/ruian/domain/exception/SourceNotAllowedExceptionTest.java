package cz.vojtechhubner.ruian.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;

class SourceNotAllowedExceptionTest {

    @Test
    void messageNamesSourceAndReason() {
        var source = URI.create("https://evil.example/kopidlno.xml.zip");

        var exception = new SourceNotAllowedException(source, "host not allowed");

        assertThat(exception)
                .hasMessage("Source https://evil.example/kopidlno.xml.zip is not allowed: host not allowed");
        assertThat(exception.source()).isEqualTo(source);
    }
}
