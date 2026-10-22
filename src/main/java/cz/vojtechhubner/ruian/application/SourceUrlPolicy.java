package cz.vojtechhubner.ruian.application;

import cz.vojtechhubner.ruian.domain.exception.SourceNotAllowedException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Decides which URLs an import may download from: only an absolute https URL without user info, on port 443 and on
 * an allowlisted host or one of its subdomains, is accepted. Together with {@code spring.http.clients.redirects:
 * dont-follow} this policy is the SSRF control of the service; the allowlist is checked on the requested URL only,
 * which is why a redirect is never followed.
 */
@Component
public class SourceUrlPolicy {

    private final List<String> allowedHosts;

    public SourceUrlPolicy(ImportProperties properties) {
        this.allowedHosts = properties.allowedHosts().stream().map(host -> host.toLowerCase(Locale.ROOT)).toList();
    }

    /**
     * @throws SourceNotAllowedException if {@code source} fails one of the checks; the reason names the first failure
     */
    public void ensureAllowed(URI source) {
        if (!source.isAbsolute()) {
            throw new SourceNotAllowedException(source, "URL must be absolute");
        }
        if (!"https".equalsIgnoreCase(source.getScheme())) {
            throw new SourceNotAllowedException(source, "only https is allowed");
        }
        if (source.getRawUserInfo() != null) {
            throw new SourceNotAllowedException(source, "user info is not allowed");
        }
        if (source.getHost() == null) {
            throw new SourceNotAllowedException(source, "host is missing or invalid");
        }
        if (source.getPort() != -1 && source.getPort() != 443) {
            throw new SourceNotAllowedException(source, "only port 443 is allowed");
        }
        String host = source.getHost().toLowerCase(Locale.ROOT);
        if (!isAllowed(host)) {
            throw new SourceNotAllowedException(source, "host " + host + " is not on the allowlist");
        }
    }

    private boolean isAllowed(String host) {
        return allowedHosts.stream().anyMatch(allowed -> host.equals(allowed) || host.endsWith("." + allowed));
    }
}
