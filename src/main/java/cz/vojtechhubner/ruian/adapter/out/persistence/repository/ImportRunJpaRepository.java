package cz.vojtechhubner.ruian.adapter.out.persistence.repository;

import cz.vojtechhubner.ruian.adapter.out.persistence.entity.ImportRunEntity;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data access to {@link ImportRunEntity} rows.
 */
public interface ImportRunJpaRepository extends JpaRepository<ImportRunEntity, Long> {

    /** Returns the most recently started runs first; runs started at the same instant come newest id first. */
    List<ImportRunEntity> findByOrderByStartedAtDescIdDesc(Limit limit);
}
