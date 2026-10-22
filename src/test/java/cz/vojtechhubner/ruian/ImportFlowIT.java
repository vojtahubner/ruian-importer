package cz.vojtechhubner.ruian;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cz.vojtechhubner.ruian.adapter.in.startup.StartupImportRunner;
import cz.vojtechhubner.ruian.application.ImportProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Runs the import flow end to end against the real application: an embedded web server, PostgreSQL from
 * Testcontainers and the VFR source replaced by a mock HTTP server that answers with the Kopidlno fixture. Every test
 * starts with empty tables and no pending expectations, so the tests depend neither on each other nor on their order.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockRestServiceServer
@AutoConfigureRestTestClient
@ActiveProfiles("test")
class ImportFlowIT {

    private static final int KOPIDLNO = 573060;
    private static final byte[] KOPIDLNO_ZIP = readResource("/vfr/kopidlno.xml.zip");

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ImportProperties properties;

    @Autowired
    private ApplicationContext context;

    @BeforeEach
    void resetServerAndDatabase() {
        server.reset();
        jdbcTemplate.execute("truncate table import_run, municipality_part, municipality");
    }

    @Test
    void startupImportIsDisabledInTheTestProfile() {
        assertThat(context.getBeanNamesForType(StartupImportRunner.class)).isEmpty();
    }

    @Test
    void importsKopidlnoAndExposesItThroughTheApi() {
        expectDownload(properties.defaultSourceUrl(), withSuccess(KOPIDLNO_ZIP, MediaType.APPLICATION_OCTET_STREAM));

        String location = importSucceeds();

        restTestClient.get().uri("/api/municipalities").exchange()
                .expectStatus().isOk()
                .expectBody().json("""
                        [{"code": 573060, "name": "Kopidlno"}]
                        """, JsonCompareMode.STRICT);
        restTestClient.get().uri("/api/municipalities/{code}/parts", KOPIDLNO).exchange()
                .expectStatus().isOk()
                .expectBody().json("""
                        [
                            {"code": 31801, "name": "Drahoraz", "municipalityCode": 573060},
                            {"code": 31828, "name": "Pševes", "municipalityCode": 573060},
                            {"code": 69299, "name": "Kopidlno", "municipalityCode": 573060},
                            {"code": 69302, "name": "Ledkov", "municipalityCode": 573060},
                            {"code": 97373, "name": "Mlýnec", "municipalityCode": 573060}
                        ]
                        """, JsonCompareMode.STRICT);
        restTestClient.get().uri(location).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(runId(location))
                .jsonPath("$.status").isEqualTo("SUCCEEDED")
                .jsonPath("$.municipalityCode").isEqualTo(KOPIDLNO)
                .jsonPath("$.partCount").isEqualTo(5);
        restTestClient.get().uri("/api/imports").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(1);
        server.verify();
    }

    @Test
    void secondImportIsIdempotent() {
        expectDownload(properties.defaultSourceUrl(), withSuccess(KOPIDLNO_ZIP, MediaType.APPLICATION_OCTET_STREAM));
        expectDownload(properties.defaultSourceUrl(), withSuccess(KOPIDLNO_ZIP, MediaType.APPLICATION_OCTET_STREAM));

        long firstRun = runId(importSucceeds());
        long secondRun = runId(importSucceeds());

        assertThat(rowCount("municipality")).isEqualTo(1);
        assertThat(rowCount("municipality_part")).isEqualTo(5);
        assertThat(rowCount("import_run")).isEqualTo(2);
        restTestClient.get().uri("/api/imports").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo(secondRun)
                .jsonPath("$[1].id").isEqualTo(firstRun);
        server.verify();
    }

    @Test
    void unavailableSourceIsRecordedAsFailedRun() {
        expectDownload(properties.defaultSourceUrl(), withStatus(HttpStatus.NOT_FOUND));

        var problem = restTestClient.post().uri("/api/imports").exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody();
        long runId = jdbcTemplate.queryForObject("select id from import_run", Long.class);
        problem.jsonPath("$.type").isEqualTo("urn:problem-type:source-unavailable")
                .jsonPath("$.instance").isEqualTo("/api/imports/" + runId)
                .jsonPath("$.importRunId").isEqualTo(runId);
        restTestClient.get().uri("/api/imports/{id}", runId).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("FAILED")
                .jsonPath("$.errorMessage").value(String.class, message -> assertThat(message).contains("HTTP 404"));
        assertThat(rowCount("municipality")).isZero();
    }

    @Test
    void sourceOutsideAllowlistIsRejectedWithoutARun() {
        restTestClient.post().uri("/api/imports")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"sourceUrl\": \"https://example.com/x.zip\"}")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.type").isEqualTo("urn:problem-type:source-not-allowed");

        assertThat(rowCount("import_run")).isZero();
        server.verify();
    }

    @Test
    void healthIncludesDatabase() {
        restTestClient.get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.components.db.status").isEqualTo("UP");
    }

    @Test
    void openApiDescribesTheImportEndpoint() {
        restTestClient.get().uri("/v3/api-docs").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body).contains("\"/api/imports\""));
    }

    private void expectDownload(URI url, ResponseCreator response) {
        server.expect(requestTo(url.toString())).andRespond(response);
    }

    /** Imports the default source, checks the recorded run and returns the {@code Location} of that run. */
    private String importSucceeds() {
        var result = restTestClient.post().uri("/api/imports").exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", "/api/imports/\\d+")
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCEEDED")
                .jsonPath("$.municipalityCode").isEqualTo(KOPIDLNO)
                .jsonPath("$.partCount").isEqualTo(5)
                .jsonPath("$.errorMessage").isEqualTo(null)
                .returnResult();
        return Objects.requireNonNull(result.getResponseHeaders().getFirst("Location"));
    }

    private static long runId(String location) {
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private long rowCount(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
    }

    private static byte[] readResource(String path) {
        try (var in = Objects.requireNonNull(ImportFlowIT.class.getResourceAsStream(path), path)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
