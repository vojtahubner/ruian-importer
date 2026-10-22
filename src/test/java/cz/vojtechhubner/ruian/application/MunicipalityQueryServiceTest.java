package cz.vojtechhubner.ruian.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import cz.vojtechhubner.ruian.domain.exception.NotFoundException;
import cz.vojtechhubner.ruian.domain.model.Municipality;
import cz.vojtechhubner.ruian.domain.model.MunicipalityPart;
import cz.vojtechhubner.ruian.domain.port.out.MunicipalityStore;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MunicipalityQueryServiceTest {

    private static final Municipality KOPIDLNO = new Municipality(573060, "Kopidlno");
    private static final int UNKNOWN = 999999;

    @Mock
    private MunicipalityStore municipalities;

    private MunicipalityQueryService service;

    @BeforeEach
    void setUp() {
        service = new MunicipalityQueryService(municipalities);
    }

    @Test
    void allComesFromTheStore() {
        when(municipalities.findAll()).thenReturn(List.of(KOPIDLNO));

        assertThat(service.all()).containsExactly(KOPIDLNO);
    }

    @Test
    void byCodeReturnsTheStoredMunicipality() {
        when(municipalities.findByCode(KOPIDLNO.code())).thenReturn(Optional.of(KOPIDLNO));

        assertThat(service.byCode(KOPIDLNO.code())).isEqualTo(KOPIDLNO);
    }

    @Test
    void byCodeOfUnknownMunicipalityIsNotFound() {
        when(municipalities.findByCode(UNKNOWN)).thenReturn(Optional.empty());

        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> service.byCode(UNKNOWN))
                .withMessage("Municipality 999999 not found")
                .satisfies(e -> {
                    assertThat(e.resource()).isEqualTo("Municipality");
                    assertThat(e.id()).isEqualTo(UNKNOWN);
                });
    }

    @Test
    void partsOfKnownMunicipalityComeFromTheStore() {
        var parts = List.of(
                new MunicipalityPart(69299, "Kopidlno", KOPIDLNO.code()),
                new MunicipalityPart(69302, "Ledkov", KOPIDLNO.code()));
        when(municipalities.findByCode(KOPIDLNO.code())).thenReturn(Optional.of(KOPIDLNO));
        when(municipalities.findPartsByMunicipalityCode(KOPIDLNO.code())).thenReturn(parts);

        assertThat(service.partsOf(KOPIDLNO.code())).isEqualTo(parts);
    }

    @Test
    void partsOfUnknownMunicipalityIsNotFound() {
        when(municipalities.findByCode(UNKNOWN)).thenReturn(Optional.empty());

        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> service.partsOf(UNKNOWN))
                .withMessage("Municipality 999999 not found");
    }
}
