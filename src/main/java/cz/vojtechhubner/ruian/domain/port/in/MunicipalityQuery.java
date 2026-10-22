package cz.vojtechhubner.ruian.domain.port.in;

import cz.vojtechhubner.ruian.domain.exception.NotFoundException;
import cz.vojtechhubner.ruian.domain.model.Municipality;
import cz.vojtechhubner.ruian.domain.model.MunicipalityPart;
import java.util.List;

/**
 * Read access to the imported municipalities and their parts.
 */
public interface MunicipalityQuery {

    List<Municipality> all();

    /**
     * @throws NotFoundException if no municipality has the given code
     */
    Municipality byCode(int code);

    /**
     * @throws NotFoundException if no municipality has the given code
     */
    List<MunicipalityPart> partsOf(int municipalityCode);
}
