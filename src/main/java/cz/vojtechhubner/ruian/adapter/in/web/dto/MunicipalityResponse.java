package cz.vojtechhubner.ruian.adapter.in.web.dto;

import cz.vojtechhubner.ruian.domain.model.Municipality;

/**
 * A municipality as exposed by the API.
 */
public record MunicipalityResponse(int code, String name) {

    public static MunicipalityResponse from(Municipality municipality) {
        return new MunicipalityResponse(municipality.code(), municipality.name());
    }
}
