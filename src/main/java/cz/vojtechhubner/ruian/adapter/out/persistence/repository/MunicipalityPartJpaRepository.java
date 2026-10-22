package cz.vojtechhubner.ruian.adapter.out.persistence.repository;

import cz.vojtechhubner.ruian.adapter.out.persistence.entity.MunicipalityPartEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data access to {@link MunicipalityPartEntity} rows.
 */
public interface MunicipalityPartJpaRepository extends JpaRepository<MunicipalityPartEntity, Integer> {

    List<MunicipalityPartEntity> findByMunicipalityCodeOrderByCode(int municipalityCode);
}
