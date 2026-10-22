package cz.vojtechhubner.ruian.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cz.vojtechhubner.ruian.domain.exception.NotFoundException;
import cz.vojtechhubner.ruian.domain.model.Municipality;
import cz.vojtechhubner.ruian.domain.model.MunicipalityPart;
import cz.vojtechhubner.ruian.domain.port.in.MunicipalityQuery;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MunicipalityController.class)
class MunicipalityControllerTest {

    private static final Municipality KOPIDLNO = new Municipality(573060, "Kopidlno");
    private static final int UNKNOWN = 999999;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MunicipalityQuery municipalities;

    @Test
    void listsAllMunicipalities() throws Exception {
        when(municipalities.all()).thenReturn(List.of(KOPIDLNO));

        mockMvc.perform(get("/api/municipalities"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[{\"code\": 573060, \"name\": \"Kopidlno\"}]"));
    }

    @Test
    void returnsMunicipalityByCode() throws Exception {
        when(municipalities.byCode(573060)).thenReturn(KOPIDLNO);

        mockMvc.perform(get("/api/municipalities/573060"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(573060))
                .andExpect(jsonPath("$.name").value("Kopidlno"));
    }

    @Test
    void unknownMunicipalityIsNotFound() throws Exception {
        when(municipalities.byCode(UNKNOWN)).thenThrow(new NotFoundException("Municipality", UNKNOWN));

        mockMvc.perform(get("/api/municipalities/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:not-found"))
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.detail").value("Municipality 999999 not found"));
    }

    @Test
    void listsPartsOfMunicipality() throws Exception {
        when(municipalities.partsOf(573060)).thenReturn(List.of(
                new MunicipalityPart(69299, "Kopidlno", 573060),
                new MunicipalityPart(69302, "Ledkov", 573060)));

        mockMvc.perform(get("/api/municipalities/573060/parts"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        [{"code": 69299, "name": "Kopidlno", "municipalityCode": 573060},
                         {"code": 69302, "name": "Ledkov", "municipalityCode": 573060}]"""));
    }

    @Test
    void partsOfUnknownMunicipalityAreNotFound() throws Exception {
        when(municipalities.partsOf(UNKNOWN)).thenThrow(new NotFoundException("Municipality", UNKNOWN));

        mockMvc.perform(get("/api/municipalities/999999/parts"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem-type:not-found"))
                .andExpect(jsonPath("$.detail").value("Municipality 999999 not found"));
    }

    @Test
    void unexpectedFailureIsInternalErrorWithoutTheExceptionMessage() throws Exception {
        when(municipalities.all()).thenThrow(new RuntimeException("connection to db-host:5432 refused"));

        mockMvc.perform(get("/api/municipalities"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:internal-error"))
                .andExpect(jsonPath("$.title").value("Internal error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }
}
