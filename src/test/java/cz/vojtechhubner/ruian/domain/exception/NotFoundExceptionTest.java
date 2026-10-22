package cz.vojtechhubner.ruian.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotFoundExceptionTest {

    @Test
    void messageNamesResourceAndId() {
        var exception = new NotFoundException("Municipality", 573060);

        assertThat(exception).hasMessage("Municipality 573060 not found");
        assertThat(exception.resource()).isEqualTo("Municipality");
        assertThat(exception.id()).isEqualTo(573060);
    }
}
