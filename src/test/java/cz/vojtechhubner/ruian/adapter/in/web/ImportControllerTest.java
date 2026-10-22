package cz.vojtechhubner.ruian.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cz.vojtechhubner.ruian.domain.exception.ImportFailedException;
import cz.vojtechhubner.ruian.domain.exception.NotFoundException;
import cz.vojtechhubner.ruian.domain.exception.SourceNotAllowedException;
import cz.vojtechhubner.ruian.domain.exception.SourceUnavailableException;
import cz.vojtechhubner.ruian.domain.exception.VfrParseException;
import cz.vojtechhubner.ruian.domain.model.ImportRun;
import cz.vojtechhubner.ruian.domain.port.in.ImportMunicipalityUseCase;
import cz.vojtechhubner.ruian.domain.port.in.ImportRunQuery;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImportController.class)
class ImportControllerTest {

    private static final URI SOURCE = URI.create("https://www.smartform.cz/download/kopidlno.xml.zip");
    private static final Instant STARTED_AT = Instant.parse("2026-10-22T10:00:00Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-10-22T10:00:05Z");
    private static final ImportRun SUCCEEDED_RUN =
            ImportRun.succeeded(SOURCE, STARTED_AT, FINISHED_AT, 573060, 5).withId(42L);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportMunicipalityUseCase importMunicipality;

    @MockitoBean
    private ImportRunQuery importRuns;

    @Test
    void postWithoutBodyImportsFromTheDefaultSource() throws Exception {
        when(importMunicipality.importFrom(null)).thenReturn(SUCCEEDED_RUN);

        mockMvc.perform(post("/api/imports"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/imports/42"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.municipalityCode").value(573060))
                .andExpect(jsonPath("$.partCount").value(5));

        verify(importMunicipality).importFrom(null);
    }

    @Test
    void postWithBlankSourceUrlImportsFromTheDefaultSource() throws Exception {
        when(importMunicipality.importFrom(null)).thenReturn(SUCCEEDED_RUN);

        mockMvc.perform(post("/api/imports").contentType(MediaType.APPLICATION_JSON).content("{\"sourceUrl\": \" \"}"))
                .andExpect(status().isCreated());

        verify(importMunicipality).importFrom(null);
    }

    @Test
    void postWithSourceUrlImportsFromThatSource() throws Exception {
        var source = URI.create("https://vdp.cuzk.gov.cz/x.zip");
        when(importMunicipality.importFrom(source)).thenReturn(SUCCEEDED_RUN);

        mockMvc.perform(post("/api/imports").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceUrl\": \"https://vdp.cuzk.gov.cz/x.zip\"}"))
                .andExpect(status().isCreated());

        verify(importMunicipality).importFrom(source);
    }

    @Test
    void postWithMalformedSourceUrlIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/imports").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceUrl\": \"http://exa mple.com/x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("sourceUrl is not a valid URI"));

        verifyNoInteractions(importMunicipality);
    }

    @Test
    void postWithTooLongSourceUrlIsBadRequest() throws Exception {
        var body = "{\"sourceUrl\": \"" + "x".repeat(2049) + "\"}";

        mockMvc.perform(post("/api/imports").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(importMunicipality);
    }

    @Test
    void sourceNotAllowedIsUnprocessableAndPointsAtNoRun() throws Exception {
        var source = URI.create("https://example.com/x.zip");
        when(importMunicipality.importFrom(source))
                .thenThrow(new SourceNotAllowedException(source, "host example.com is not allowed"));

        mockMvc.perform(post("/api/imports").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceUrl\": \"https://example.com/x.zip\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:source-not-allowed"))
                .andExpect(jsonPath("$.title").value("Source not allowed"))
                .andExpect(jsonPath("$.detail")
                        .value("Source https://example.com/x.zip is not allowed: host example.com is not allowed"))
                // Spring MVC fills a missing instance with the request path, so the problem points at no run
                .andExpect(jsonPath("$.instance").value("/api/imports"))
                .andExpect(jsonPath("$.importRunId").doesNotExist());
    }

    @Test
    void sourceNotAllowedProblemLeavesInstanceUnset() {
        // The handler sets no instance itself; Spring MVC fills in the request path afterwards (test above)
        var source = URI.create("https://example.com/x.zip");
        var problem = new ApiExceptionHandler()
                .handleSourceNotAllowed(new SourceNotAllowedException(source, "host example.com is not allowed"));

        assertThat(problem.getInstance()).isNull();
    }

    @Test
    void importFailedByUnavailableSourceIsBadGatewayPointingAtTheFailedRun() throws Exception {
        var cause = new SourceUnavailableException(SOURCE, "HTTP 404");
        when(importMunicipality.importFrom(null))
                .thenThrow(new ImportFailedException(failedRun(cause.getMessage()), cause));

        mockMvc.perform(post("/api/imports"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:source-unavailable"))
                .andExpect(jsonPath("$.title").value("Source unavailable"))
                .andExpect(jsonPath("$.detail").value(cause.getMessage()))
                .andExpect(jsonPath("$.instance").value("/api/imports/7"))
                .andExpect(jsonPath("$.importRunId").value(7));
    }

    @Test
    void importFailedByInvalidVfrIsUnprocessable() throws Exception {
        var cause = new VfrParseException("unexpected element <Foo>");
        when(importMunicipality.importFrom(null))
                .thenThrow(new ImportFailedException(failedRun(cause.getMessage()), cause));

        mockMvc.perform(post("/api/imports"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value("urn:problem-type:invalid-vfr"))
                .andExpect(jsonPath("$.title").value("Invalid VFR data"))
                .andExpect(jsonPath("$.detail").value("unexpected element <Foo>"))
                .andExpect(jsonPath("$.instance").value("/api/imports/7"))
                .andExpect(jsonPath("$.importRunId").value(7));
    }

    @Test
    void importFailedUnexpectedlyIsInternalErrorWithoutTheCauseMessage() throws Exception {
        var cause = new RuntimeException("connection to db-host:5432 refused");
        when(importMunicipality.importFrom(null))
                .thenThrow(new ImportFailedException(failedRun("Unexpected RuntimeException (see server log)"), cause));

        mockMvc.perform(post("/api/imports"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("urn:problem-type:import-failed"))
                .andExpect(jsonPath("$.title").value("Import failed"))
                .andExpect(jsonPath("$.detail").value("The import failed unexpectedly; see the server log."))
                .andExpect(jsonPath("$.instance").value("/api/imports/7"))
                .andExpect(jsonPath("$.importRunId").value(7));
    }

    @Test
    void listRejectsLimitOutOfRange() throws Exception {
        mockMvc.perform(get("/api/imports").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/api/imports").param("limit", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(importRuns);
    }

    @Test
    void listPassesTheLimitToTheQuery() throws Exception {
        when(importRuns.latest(2)).thenReturn(List.of(SUCCEEDED_RUN));

        mockMvc.perform(get("/api/imports").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(42));

        verify(importRuns).latest(2);
    }

    @Test
    void listDefaultsToTwentyRuns() throws Exception {
        mockMvc.perform(get("/api/imports"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(importRuns).latest(20);
    }

    @Test
    void unknownRunIsNotFound() throws Exception {
        when(importRuns.byId(9L)).thenThrow(new NotFoundException("Import run", 9L));

        mockMvc.perform(get("/api/imports/9"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:not-found"))
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.detail").value("Import run 9 not found"));
    }

    @Test
    void knownRunIsReturnedWithItsFields() throws Exception {
        when(importRuns.byId(9L)).thenReturn(SUCCEEDED_RUN.withId(9L));

        mockMvc.perform(get("/api/imports/9"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.sourceUrl").value("https://www.smartform.cz/download/kopidlno.xml.zip"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.startedAt").value("2026-10-22T10:00:00Z"))
                .andExpect(jsonPath("$.finishedAt").value("2026-10-22T10:00:05Z"))
                .andExpect(jsonPath("$.municipalityCode").value(573060))
                .andExpect(jsonPath("$.partCount").value(5));
    }

    @Test
    void failedRunExposesItsErrorMessage() throws Exception {
        when(importRuns.byId(7L)).thenReturn(failedRun("Source https://example.com/x.zip is unavailable: HTTP 404"));

        mockMvc.perform(get("/api/imports/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.partCount").value(0))
                .andExpect(jsonPath("$.errorMessage")
                        .value("Source https://example.com/x.zip is unavailable: HTTP 404"));
    }

    private static ImportRun failedRun(String errorMessage) {
        return ImportRun.failed(SOURCE, STARTED_AT, FINISHED_AT, errorMessage).withId(7L);
    }
}
