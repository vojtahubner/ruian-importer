package cz.vojtechhubner.ruian.config;

import org.springframework.boot.http.client.InetAddressFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Restricts where outbound HTTP clients may connect. Boot applies the {@link InetAddressFilter} to every
 * auto-configured HTTP client; it rejects loopback, private and other non-public addresses once per request and is
 * defence in depth behind the host allowlist and the disabled redirects ({@code spring.http.clients.redirects}
 * is {@code dont-follow} in application.yaml).
 * <p>
 * Nothing else is customised on purpose: there is no custom request factory and no {@code RestClient} bean. The
 * auto-configured {@code RestClient.Builder} must stay the only way clients are created, otherwise
 * {@code MockRestServiceServer} cannot intercept them in tests and the {@code spring.http.clients.*} timeouts are
 * lost.
 */
@Configuration(proxyBeanMethods = false)
public class HttpClientConfig {

    @Bean
    InetAddressFilter httpClientInetAddressFilter() {
        return InetAddressFilter.externalAddresses();
    }
}
