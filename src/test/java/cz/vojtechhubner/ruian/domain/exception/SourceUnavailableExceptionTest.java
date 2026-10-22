package cz.vojtechhubner.ruian.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.Test;

class SourceUnavailableExceptionTest {

    private static final URI SOURCE = URI.create("https://www.smartform.cz/kopidlno.xml.zip");

    @Test
    void messageNamesSourceAndReason() {
        var exception = new SourceUnavailableException(SOURCE, "HTTP 503");

        assertThat(exception)
                .hasMessage("Source https://www.smartform.cz/kopidlno.xml.zip is unavailable: HTTP 503")
                .hasNoCause();
        assertThat(exception.source()).isEqualTo(SOURCE);
    }

    @Test
    void keepsCause() {
        var cause = new IOException("connection reset");

        var exception = new SourceUnavailableException(SOURCE, "connection reset", cause);

        assertThat(exception)
                .hasMessage("Source https://www.smartform.cz/kopidlno.xml.zip is unavailable: connection reset")
                .hasCause(cause);
    }
}
