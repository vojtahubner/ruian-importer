package cz.vojtechhubner.ruian.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Row of the {@code municipality_part} table. The owning municipality is referenced by its code only; the foreign
 * key and its cascade live in the database.
 */
@Entity
@Table(name = "municipality_part")
@EntityListeners(AuditingEntityListener.class)
public class MunicipalityPartEntity {

    @Id
    private Integer code;

    @Column(nullable = false, length = 48)
    private String name;

    private int municipalityCode;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private @Nullable Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private @Nullable Instant updatedAt;

    /** Required by JPA. */
    protected MunicipalityPartEntity() {
    }

    public MunicipalityPartEntity(int code, String name, int municipalityCode) {
        this.code = code;
        this.name = name;
        this.municipalityCode = municipalityCode;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMunicipalityCode() {
        return municipalityCode;
    }

    public void setMunicipalityCode(int municipalityCode) {
        this.municipalityCode = municipalityCode;
    }

    public @Nullable Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Instant getUpdatedAt() {
        return updatedAt;
    }
}
