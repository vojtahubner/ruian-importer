package cz.vojtechhubner.ruian.adapter.out.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import cz.vojtechhubner.ruian.domain.exception.VfrParseException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class BoundedInputStreamTest {

    private static BoundedInputStream bounded(int available, long maxBytes) {
        return new BoundedInputStream(new ByteArrayInputStream(new byte[available]), maxBytes);
    }

    @Test
    void passesExactlyMaxBytes() throws IOException {
        var in = bounded(4, 4);

        assertThat(in.readAllBytes()).hasSize(4);
    }

    @Test
    void rejectsTheByteBeyondTheLimit() throws IOException {
        var in = bounded(5, 4);
        for (int i = 0; i < 4; i++) {
            in.read();
        }

        assertThatExceptionOfType(VfrParseException.class)
                .isThrownBy(in::read)
                .withMessage("Input exceeds the limit of 4 bytes");
    }

    @Test
    void rejectsAnArrayReadCrossingTheLimit() {
        var in = bounded(5, 4);

        assertThatExceptionOfType(VfrParseException.class)
                .isThrownBy(() -> in.read(new byte[8], 0, 8))
                .withMessageContaining("limit of 4 bytes");
    }

    @Test
    void countsSkippedBytes() throws IOException {
        var in = bounded(8, 4);
        in.skip(3);

        assertThat(in.read()).isZero();
        assertThatExceptionOfType(VfrParseException.class).isThrownBy(in::read);
    }
}
