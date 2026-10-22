package cz.vojtechhubner.ruian.application;

import cz.vojtechhubner.ruian.domain.exception.NotFoundException;
import cz.vojtechhubner.ruian.domain.model.Municipality;
import cz.vojtechhubner.ruian.domain.model.MunicipalityPart;
import cz.vojtechhubner.ruian.domain.port.in.MunicipalityQuery;
import cz.vojtechhubner.ruian.domain.port.out.MunicipalityStore;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Answers the municipality queries from the store. The parts of an unknown municipality are reported as a
 * {@link NotFoundException} rather than as an empty list, so that a wrong code is not mistaken for a municipality
 * without parts.
 */
@Service
public class MunicipalityQueryService implements MunicipalityQuery {

    private final MunicipalityStore municipalities;

    public MunicipalityQueryService(MunicipalityStore municipalities) {
        this.municipalities = municipalities;
    }

    @Override
    public List<Municipality> all() {
        return municipalities.findAll();
    }

    @Override
    public Municipality byCode(int code) {
        return municipalities.findByCode(code).orElseThrow(() -> new NotFoundException("Municipality", code));
    }

    @Override
    public List<MunicipalityPart> partsOf(int municipalityCode) {
        byCode(municipalityCode);
        return municipalities.findPartsByMunicipalityCode(municipalityCode);
    }
}
