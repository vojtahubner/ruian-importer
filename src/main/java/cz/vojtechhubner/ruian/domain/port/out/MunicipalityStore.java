package cz.vojtechhubner.ruian.domain.port.out;

import cz.vojtechhubner.ruian.domain.model.Municipality;
import cz.vojtechhubner.ruian.domain.model.MunicipalityDataset;
import cz.vojtechhubner.ruian.domain.model.MunicipalityPart;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of municipalities and their parts.
 */
public interface MunicipalityStore {

    /**
     * Upserts the dataset's municipality and parts and deletes the parts that are no longer present.
     *
     * @return the number of parts stored
     */
    int replace(MunicipalityDataset dataset);

    List<Municipality> findAll();

    Optional<Municipality> findByCode(int code);

    List<MunicipalityPart> findPartsByMunicipalityCode(int municipalityCode);
}
