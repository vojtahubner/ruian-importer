package cz.vojtechhubner.ruian.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the API for springdoc; the version comes from the Maven build info when the application was packaged
 * and falls back to "dev" otherwise (for example when run from an IDE).
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI ruianImporterOpenApi(ObjectProvider<BuildProperties> build) {
        BuildProperties buildProperties = build.getIfAvailable();
        return new OpenAPI().info(new Info()
                .title("ruian-importer API")
                .description("Imports RÚIAN municipalities and their parts from VFR XML into PostgreSQL.")
                .version(buildProperties == null ? "dev" : buildProperties.getVersion()));
    }
}
