package cz.vojtechhubner.ruian.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

class ImportPropertiesTest {

    private static final String[] VALID_VALUES = {
            "ruian.import.default-source-url=https://www.smartform.cz/download/kopidlno.xml.zip",
            "ruian.import.allowed-hosts=smartform.cz,vdp.cuzk.gov.cz",
            "ruian.import.on-startup=true",
            "ruian.import.max-uncompressed-bytes=64MB"};

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsAllValues() {
        runner.withPropertyValues(VALID_VALUES).run(context -> {
            assertThat(context).hasSingleBean(ImportProperties.class);
            var properties = context.getBean(ImportProperties.class);
            assertThat(properties.defaultSourceUrl())
                    .isEqualTo(URI.create("https://www.smartform.cz/download/kopidlno.xml.zip"));
            assertThat(properties.allowedHosts()).containsExactly("smartform.cz", "vdp.cuzk.gov.cz");
            assertThat(properties.onStartup()).isTrue();
            assertThat(properties.maxUncompressedBytes()).isEqualTo(DataSize.ofMegabytes(64));
        });
    }

    @Test
    void bindsMegabytesToBytes() {
        runner.withPropertyValues(VALID_VALUES).run(context ->
                assertThat(context.getBean(ImportProperties.class).maxUncompressedBytes().toBytes())
                        .isEqualTo(64L * 1024 * 1024));
    }

    @Test
    void rejectsEmptyAllowedHosts() {
        runner.withPropertyValues(VALID_VALUES).withPropertyValues("ruian.import.allowed-hosts=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause()
                    .isInstanceOf(BindValidationException.class)
                    .hasMessageContaining("allowedHosts");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ImportProperties.class)
    static class PropertiesConfiguration {
    }
}
