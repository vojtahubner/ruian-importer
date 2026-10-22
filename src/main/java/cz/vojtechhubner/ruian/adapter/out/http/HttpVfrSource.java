package cz.vojtechhubner.ruian.adapter.out.http;

import cz.vojtechhubner.ruian.domain.exception.SourceUnavailableException;
import cz.vojtechhubner.ruian.domain.port.out.VfrSource;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Downloads a VFR file over HTTP and hands the response body to the reader as it arrives, execute-around style:
 * the response is closed by {@link RestClient} after the callback returns, and nothing is buffered to disk or
 * memory. The client comes from the auto-configured builder, so the {@code spring.http.clients.*} settings apply.
 * <p>
 * Any status other than 2xx is reported as a {@link SourceUnavailableException}, a 3xx included: redirects are
 * not followed by configuration, so the message names the host a redirect points to instead. A
 * {@link ResourceAccessException} (connect or read timeout, DNS failure, I/O error while streaming) and an
 * {@link UncheckedIOException} thrown by the reader when the stream breaks mid-parse become a
 * {@link SourceUnavailableException} too; domain exceptions thrown by the reader propagate unchanged.
 */
@Component
public class HttpVfrSource implements VfrSource {

    private final RestClient restClient;

    public HttpVfrSource(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public <T> T read(URI source, Reader<T> reader) {
        try {
            return restClient.get().uri(source).exchange((request, response) -> {
                HttpStatusCode status = response.getStatusCode();
                if (!status.is2xxSuccessful()) {
                    throw new SourceUnavailableException(source, describe(status, response.getHeaders().getLocation()));
                }
                try (InputStream body = response.getBody()) {
                    return reader.read(body);
                }
            });
        } catch (ResourceAccessException e) {
            throw new SourceUnavailableException(source, "connection failed: " + e.getMessage(), e);
        } catch (UncheckedIOException e) {
            throw new SourceUnavailableException(source, "download interrupted: " + e.getMessage(), e);
        }
    }

    private static String describe(HttpStatusCode status, @Nullable URI location) {
        var description = "HTTP " + status.value();
        if (!status.is3xxRedirection()) {
            return description;
        }
        return location == null
                ? description + " redirect without Location"
                : description + " redirect to " + location.getHost();
    }
}
