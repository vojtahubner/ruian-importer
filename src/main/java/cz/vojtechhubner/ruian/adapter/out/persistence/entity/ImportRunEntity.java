package cz.vojtechhubner.ruian.adapter.out.persistence.entity;

import cz.vojtechhubner.ruian.domain.model.ImportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Row of the {@code import_run} table: one import attempt, inserted once the run has finished and never updated.
 */
@Entity
@Table(name = "import_run")
public class ImportRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Column(nullable = false, length = 2048)
    private String sourceUrl;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ImportStatus status;

    private @Nullable Integer municipalityCode;

    private int partCount;

    @Column(length = 1000)
    private @Nullable String errorMessage;

    /** Required by JPA. */
    protected ImportRunEntity() {
    }

    public ImportRunEntity(
            String sourceUrl,
            Instant startedAt,
            Instant finishedAt,
            ImportStatus status,
            @Nullable Integer municipalityCode,
            int partCount,
            @Nullable String errorMessage) {
        this.sourceUrl = sourceUrl;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.status = status;
        this.municipalityCode = municipalityCode;
        this.partCount = partCount;
        this.errorMessage = errorMessage;
    }

    public @Nullable Long getId() {
        return id;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public ImportStatus getStatus() {
        return status;
    }

    public @Nullable Integer getMunicipalityCode() {
        return municipalityCode;
    }

    public int getPartCount() {
        return partCount;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }
}
