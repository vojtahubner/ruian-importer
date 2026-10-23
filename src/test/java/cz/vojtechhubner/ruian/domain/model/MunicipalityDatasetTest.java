package cz.vojtechhubner.ruian.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MunicipalityDatasetTest {

    private static final Municipality KOPIDLNO = new Municipality(573060, "Kopidlno");
    private static final MunicipalityPart KOPIDLNO_PART = new MunicipalityPart(69205, "Kopidlno", 573060);
    private static final MunicipalityPart PSEVES = new MunicipalityPart(69213, "Pševes", 573060);

    @Test
    void copiesPartsDefensively() {
        var parts = new ArrayList<>(List.of(KOPIDLNO_PART));

        var dataset = new MunicipalityDataset(KOPIDLNO, parts);
        parts.add(PSEVES);

        assertThat(dataset.parts()).containsExactly(KOPIDLNO_PART);
    }

    @Test
    void rejectsNullMunicipality() {
        assertThatNullPointerException()
                .isThrownBy(() -> new MunicipalityDataset(null, List.of(KOPIDLNO_PART)))
                .withMessage("municipality must not be null");
    }

    @Test
    void rejectsPartOfAnotherMunicipalityNamingItsCode() {
        var foreignPart = new MunicipalityPart(400050, "Jičín", 572659);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MunicipalityDataset(KOPIDLNO, List.of(KOPIDLNO_PART, foreignPart)))
                .withMessage("Part 400050 belongs to municipality 572659, expected 573060");
    }

    @Test
    void rejectsDuplicatePartCodeNamingIt() {
        var duplicate = new MunicipalityPart(KOPIDLNO_PART.code(), "Kopidlno again", 573060);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MunicipalityDataset(KOPIDLNO, List.of(KOPIDLNO_PART, PSEVES, duplicate)))
                .withMessage("Duplicate part code 69205");
    }

    @Test
    void countsParts() {
        assertThat(new MunicipalityDataset(KOPIDLNO, List.of()).partCount()).isZero();
        assertThat(new MunicipalityDataset(KOPIDLNO, List.of(KOPIDLNO_PART, PSEVES)).partCount()).isEqualTo(2);
    }
}
