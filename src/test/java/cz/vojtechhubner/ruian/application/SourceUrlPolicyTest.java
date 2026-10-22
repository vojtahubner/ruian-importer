package cz.vojtechhubner.ruian.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import cz.vojtechhubner.ruian.domain.exception.SourceNotAllowedException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.unit.DataSize;

class SourceUrlPolicyTest {

    private final SourceUrlPolicy policy = new SourceUrlPolicy(new ImportProperties(
            URI.create("https://www.smartform.cz/download/kopidlno.xml.zip"),
            List.of("smartform.cz", "vdp.cuzk.gov.cz"),
            false,
            DataSize.ofMegabytes(64)));

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.smartform.cz/download/kopidlno.xml.zip",
            "https://WWW.SMARTFORM.CZ/x.zip",
            "https://vdp.cuzk.gov.cz:443/x.zip",
            "https://smartform.cz/x.zip"})
    void acceptsHttpsUrlsOnAllowedHosts(String url) {
        assertThatCode(() -> policy.ensureAllowed(URI.create(url))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            /x.zip                                   | URL must be absolute
            http://www.smartform.cz/x.zip            | only https is allowed
            https://user:pw@www.smartform.cz/x.zip   | user info is not allowed
            https://www.smartform.cz:8443/x.zip      | only port 443 is allowed
            https://[::1]/x.zip                      | host [::1] is not on the allowlist
            https://10.0.0.1/x.zip                   | host 10.0.0.1 is not on the allowlist
            https://evilsmartform.cz/x.zip           | host evilsmartform.cz is not on the allowlist
            https://www.smartform.cz.evil.com/x.zip  | host www.smartform.cz.evil.com is not on the allowlist
            https://vdp.cuzk.cz/x.zip                | host vdp.cuzk.cz is not on the allowlist
            """)
    void rejectsWithReason(String url, String reason) {
        assertThatExceptionOfType(SourceNotAllowedException.class)
                .isThrownBy(() -> policy.ensureAllowed(URI.create(url)))
                .withMessage("Source " + url + " is not allowed: " + reason);
    }

    @Test
    void rejectsHostThatDoesNotParseAsMissing() {
        var source = URI.create("https://пример.рф/x.zip");

        assertThatExceptionOfType(SourceNotAllowedException.class)
                .isThrownBy(() -> policy.ensureAllowed(source))
                .withMessage("Source https://пример.рф/x.zip is not allowed: host is missing or invalid");
    }
}
