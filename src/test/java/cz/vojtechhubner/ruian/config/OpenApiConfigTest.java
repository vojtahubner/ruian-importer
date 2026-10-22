package cz.vojtechhubner.ruian.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;

@ExtendWith(MockitoExtension.class)
class OpenApiConfigTest {

    @Mock
    private ObjectProvider<BuildProperties> build;

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void describesTheApiWithTheBuildVersion() {
        var properties = new Properties();
        properties.setProperty("version", "0.1.0-SNAPSHOT");
        when(build.getIfAvailable()).thenReturn(new BuildProperties(properties));

        var info = config.ruianImporterOpenApi(build).getInfo();

        assertThat(info.getTitle()).isEqualTo("ruian-importer API");
        assertThat(info.getDescription())
                .isEqualTo("Imports RÚIAN municipalities and their parts from VFR XML into PostgreSQL.");
        assertThat(info.getVersion()).isEqualTo("0.1.0-SNAPSHOT");
    }

    @Test
    void fallsBackToDevVersionWithoutBuildInfo() {
        when(build.getIfAvailable()).thenReturn(null);

        assertThat(config.ruianImporterOpenApi(build).getInfo().getVersion()).isEqualTo("dev");
    }
}
