package cz.vojtechhubner.ruian.adapter.out.xml;

import static org.assertj.core.api.Assertions.assertThat;

import cz.vojtechhubner.ruian.domain.model.Municipality;
import cz.vojtechhubner.ruian.domain.model.MunicipalityPart;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class StaxVfrParserRealFileTest {

    @Test
    void parsesTheRealArchiveWithoutReadingItWhole() throws IOException {
        try (var resource = getClass().getResourceAsStream("/vfr/kopidlno.xml.zip");
                var counting = new CountingInputStream(resource)) {
            var dataset = new StaxVfrParser(64L * 1024 * 1024).parse(counting);

            assertThat(dataset.municipality()).isEqualTo(new Municipality(573060, "Kopidlno"));
            assertThat(dataset.parts()).containsExactly(
                    new MunicipalityPart(69299, "Kopidlno", 573060),
                    new MunicipalityPart(69302, "Ledkov", 573060),
                    new MunicipalityPart(97373, "Mlýnec", 573060),
                    new MunicipalityPart(31801, "Drahoraz", 573060),
                    new MunicipalityPart(31828, "Pševes", 573060));
            assertThat(counting.bytesRead()).isLessThan(200_000);
        }
    }

    /** Counts the compressed bytes the parser pulls from the archive. */
    private static final class CountingInputStream extends FilterInputStream {

        private long bytesRead;

        CountingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value != -1) {
                bytesRead++;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int read = super.read(buffer, offset, length);
            if (read > 0) {
                bytesRead += read;
            }
            return read;
        }

        long bytesRead() {
            return bytesRead;
        }
    }
}
