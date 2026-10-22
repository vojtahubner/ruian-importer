package cz.vojtechhubner.ruian.adapter.in.web;

import cz.vojtechhubner.ruian.adapter.in.web.dto.MunicipalityPartResponse;
import cz.vojtechhubner.ruian.adapter.in.web.dto.MunicipalityResponse;
import cz.vojtechhubner.ruian.domain.port.in.MunicipalityQuery;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the imported municipalities and their parts.
 */
@RestController
@RequestMapping("/api/municipalities")
@Tag(name = "Municipalities")
public class MunicipalityController {

    private final MunicipalityQuery municipalities;

    public MunicipalityController(MunicipalityQuery municipalities) {
        this.municipalities = municipalities;
    }

    @GetMapping
    public List<MunicipalityResponse> list() {
        return municipalities.all().stream().map(MunicipalityResponse::from).toList();
    }

    @GetMapping("/{code}")
    public MunicipalityResponse byCode(@PathVariable int code) {
        return MunicipalityResponse.from(municipalities.byCode(code));
    }

    @GetMapping("/{code}/parts")
    public List<MunicipalityPartResponse> parts(@PathVariable int code) {
        return municipalities.partsOf(code).stream().map(MunicipalityPartResponse::from).toList();
    }
}
