package cz.vojtechhubner.ruian.adapter.out.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cz.vojtechhubner.ruian.config.HttpClientConfig;
import cz.vojtechhubner.ruian.domain.exception.SourceUnavailableException;
import cz.vojtechhubner.ruian.domain.exception.VfrParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;

@RestClientTest(HttpVfrSource.class)
@Import(HttpClientConfig.class)
class HttpVfrSourceTest {

    private static final URI SOURCE = URI.create("https://www.smartform.cz/download/kopidlno.xml.zip");

    @Autowired
    private HttpVfrSource source;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void handsBodyToReaderAndReturnsItsResult() {
        server.expect(requestTo(SOURCE)).andExpect(method(GET))
                .andRespond(withSuccess("<xml/>", MediaType.APPLICATION_XML));

        var result = source.read(SOURCE, in -> new String(in.readAllBytes(), UTF_8));

        assertThat(result).isEqualTo("<xml/>");
        server.verify();
    }

    @Test
    void reportsNotFoundAsUnavailableSource() {
        server.expect(requestTo(SOURCE)).andRespond(withResourceNotFound());

        assertThatExceptionOfType(SourceUnavailableException.class)
                .isThrownBy(() -> source.read(SOURCE, InputStream::readAllBytes))
                .withMessageContaining("HTTP 404")
                .withMessageContaining(SOURCE.toString())
                .satisfies(e -> assertThat(e.source()).isEqualTo(SOURCE));
    }

    @Test
    void reportsServerErrorAsUnavailableSource() {
        server.expect(requestTo(SOURCE)).andRespond(withServerError());

        assertThatExceptionOfType(SourceUnavailableException.class)
                .isThrownBy(() -> source.read(SOURCE, InputStream::readAllBytes))
                .withMessageContaining("HTTP 500");
    }

    @Test
    void reportsRedirectAsUnavailableSourceWithoutCallingReader() {
        server.expect(requestTo(SOURCE))
                .andRespond(withStatus(HttpStatus.FOUND).location(URI.create("https://vdp.cuzk.gov.cz/x.zip")));

        assertThatExceptionOfType(SourceUnavailableException.class)
                .isThrownBy(() -> source.read(SOURCE, in -> fail("reader must not be called")))
                .withMessageContaining("HTTP 302")
                .withMessageContaining("vdp.cuzk.gov.cz");
    }

    @Test
    void propagatesParseFailureUnchanged() {
        server.expect(requestTo(SOURCE)).andRespond(withSuccess("<xml/>", MediaType.APPLICATION_XML));
        var failure = new VfrParseException("bad");

        assertThatExceptionOfType(VfrParseException.class)
                .isThrownBy(() -> source.read(SOURCE, in -> { throw failure; }))
                .isSameAs(failure);
    }

    @Test
    void reportsBrokenStreamAsInterruptedDownload() {
        server.expect(requestTo(SOURCE)).andRespond(withSuccess("<xml/>", MediaType.APPLICATION_XML));
        var failure = new UncheckedIOException(new IOException("connection reset"));

        assertThatExceptionOfType(SourceUnavailableException.class)
                .isThrownBy(() -> source.read(SOURCE, in -> { throw failure; }))
                .withMessageContaining("download interrupted")
                .withCause(failure);
    }

    @Test
    void reportsTimeoutAsConnectionFailure() {
        server.expect(requestTo(SOURCE)).andRespond(withException(new SocketTimeoutException("read timed out")));

        assertThatExceptionOfType(SourceUnavailableException.class)
                .isThrownBy(() -> source.read(SOURCE, InputStream::readAllBytes))
                .withMessageContaining("connection failed")
                .withCauseInstanceOf(ResourceAccessException.class);
    }
}
