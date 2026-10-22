package cz.vojtechhubner.ruian.adapter.in.web;

import cz.vojtechhubner.ruian.adapter.in.web.dto.ImportRequest;
import cz.vojtechhubner.ruian.adapter.in.web.dto.ImportRunResponse;
import cz.vojtechhubner.ruian.domain.model.ImportRun;
import cz.vojtechhubner.ruian.domain.port.in.ImportMunicipalityUseCase;
import cz.vojtechhubner.ruian.domain.port.in.ImportRunQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Runs imports and exposes the recorded import runs.
 *
 * <p>Deliberately without a class-level {@code @Validated}: Spring MVC validates handler parameters itself (a 400
 * from the base class of {@link ApiExceptionHandler}) only when that annotation is absent; with it, the AOP proxy
 * would throw {@code ConstraintViolationException} instead, which would end as a 500.
 */
@RestController
@RequestMapping("/api/imports")
@Tag(name = "Imports",
        description = "Runs imports of a municipality from a RÚIAN VFR file and lists the recorded runs")
public class ImportController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final ImportMunicipalityUseCase importMunicipality;
    private final ImportRunQuery importRuns;

    public ImportController(ImportMunicipalityUseCase importMunicipality, ImportRunQuery importRuns) {
        this.importMunicipality = importMunicipality;
        this.importRuns = importRuns;
    }

    @PostMapping
    @Operation(summary = "Run an import", description = "Downloads the VFR file (default: configured Kopidlno source), "
            + "stores the municipality and its parts and records the run.")
    @ApiResponse(responseCode = "201", description = "The import succeeded; the body is the recorded run")
    @ApiResponse(responseCode = "400",
            description = "The body is unreadable or sourceUrl is too long or not a valid URI",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "The source is not allowed or the VFR data is invalid",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "502", description = "The source could not be downloaded",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ImportRunResponse> run(
            @RequestBody(required = false) @Valid @Nullable ImportRequest request) {
        ImportRun run = importMunicipality.importFrom(sourceUrl(request));
        return ResponseEntity.created(URI.create("/api/imports/" + run.id())).body(ImportRunResponse.from(run));
    }

    @GetMapping
    public List<ImportRunResponse> latest(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return importRuns.latest(limit).stream().map(ImportRunResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ImportRunResponse byId(@PathVariable long id) {
        return ImportRunResponse.from(importRuns.byId(id));
    }

    private static @Nullable URI sourceUrl(@Nullable ImportRequest request) {
        if (request == null || request.sourceUrl() == null || request.sourceUrl().isBlank()) {
            return null;
        }
        try {
            return new URI(request.sourceUrl());
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sourceUrl is not a valid URI");
        }
    }
}
