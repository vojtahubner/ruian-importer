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
 * Row of the {@code municipality} table; the audit timestamps are filled in by Spring Data auditing.
 */
@Entity
@Table(name = "municipality")
@EntityListeners(AuditingEntityListener.class)
public class MunicipalityEntity {

    @Id
    private Integer code;

    @Column(nullable = false, length = 48)
    private String name;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private @Nullable Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private @Nullable Instant updatedAt;

    /** Required by JPA. */
    protected MunicipalityEntity() {
    }

    public MunicipalityEntity(int code, String name) {
        this.code = code;
        this.name = name;
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

    public @Nullable Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Instant getUpdatedAt() {
        return updatedAt;
    }
}
