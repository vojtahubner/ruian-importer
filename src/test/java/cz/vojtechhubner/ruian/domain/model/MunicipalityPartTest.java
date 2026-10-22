package cz.vojtechhubner.ruian.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class MunicipalityPartTest {

    @Test
    void createsPartFromCodeNameAndMunicipalityCode() {
        var part = new MunicipalityPart(69205, "Kopidlno", 573060);

        assertThat(part.code()).isEqualTo(69205);
        assertThat(part.name()).isEqualTo("Kopidlno");
        assertThat(part.municipalityCode()).isEqualTo(573060);
    }

    @Test
    void rejectsNonPositiveCode() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MunicipalityPart(0, "Kopidlno", 573060))
                .withMessageContaining("code");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MunicipalityPart(-1, "Kopidlno", 573060))
                .withMessageContaining("code");
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MunicipalityPart(69205, " ", 573060))
                .withMessageContaining("name");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MunicipalityPart(69205, null, 573060))
                .withMessageContaining("name");
    }

    @Test
    void acceptsNameOf48CharactersButRejectsLonger() {
        var longestName = "a".repeat(48);

        assertThat(new MunicipalityPart(69205, longestName, 573060).name()).isEqualTo(longestName);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MunicipalityPart(69205, longestName + "b", 573060))
                .withMessageContaining("name");
    }

    @Test
    void rejectsNonPositiveMunicipalityCode() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MunicipalityPart(69205, "Kopidlno", 0))
                .withMessageContaining("municipalityCode");
    }
}
