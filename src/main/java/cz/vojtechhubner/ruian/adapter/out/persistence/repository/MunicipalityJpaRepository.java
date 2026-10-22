package cz.vojtechhubner.ruian.adapter.out.persistence.repository;

import cz.vojtechhubner.ruian.adapter.out.persistence.entity.MunicipalityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data access to {@link MunicipalityEntity} rows.
 */
public interface MunicipalityJpaRepository extends JpaRepository<MunicipalityEntity, Integer> {
}
