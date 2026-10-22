package cz.vojtechhubner.ruian.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class MunicipalityTest {

    @Test
    void createsMunicipalityFromCodeAndName() {
        var municipality = new Municipality(573060, "Kopidlno");

        assertThat(municipality.code()).isEqualTo(573060);
        assertThat(municipality.name()).isEqualTo("Kopidlno");
    }

    @Test
    void rejectsNonPositiveCode() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Municipality(0, "Kopidlno"))
                .withMessageContaining("code");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Municipality(-1, "Kopidlno"))
                .withMessageContaining("code");
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Municipality(573060, " "))
                .withMessageContaining("name");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Municipality(573060, null))
                .withMessageContaining("name");
    }

    @Test
    void acceptsNameOf48CharactersButRejectsLonger() {
        var longestName = "a".repeat(48);

        assertThat(new Municipality(573060, longestName).name()).isEqualTo(longestName);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Municipality(573060, longestName + "b"))
                .withMessageContaining("name");
    }
}
