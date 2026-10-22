package cz.vojtechhubner.ruian.adapter.in.web.dto;

import cz.vojtechhubner.ruian.domain.model.MunicipalityPart;

/**
 * A part of a municipality as exposed by the API.
 */
public record MunicipalityPartResponse(int code, String name, int municipalityCode) {

    public static MunicipalityPartResponse from(MunicipalityPart part) {
        return new MunicipalityPartResponse(part.code(), part.name(), part.municipalityCode());
    }
}
