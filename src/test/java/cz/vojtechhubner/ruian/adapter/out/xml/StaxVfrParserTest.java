package cz.vojtechhubner.ruian.adapter.out.xml;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import cz.vojtechhubner.ruian.application.ImportProperties;
import cz.vojtechhubner.ruian.domain.exception.VfrParseException;
import cz.vojtechhubner.ruian.domain.model.Municipality;
import cz.vojtechhubner.ruian.domain.model.MunicipalityDataset;
import cz.vojtechhubner.ruian.domain.model.MunicipalityPart;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class StaxVfrParserTest {

    private static final int KOPIDLNO = 573060;
    private static final List<MunicipalityPart> KOPIDLNO_PARTS = List.of(
            new MunicipalityPart(69299, "Kopidlno", KOPIDLNO),
            new MunicipalityPart(69302, "Ledkov", KOPIDLNO),
            new MunicipalityPart(97373, "Mlýnec", KOPIDLNO),
            new MunicipalityPart(31801, "Drahoraz", KOPIDLNO),
            new MunicipalityPart(31828, "Pševes", KOPIDLNO));

    private final StaxVfrParser parser = parserWithLimit(64L * 1024 * 1024);

    /** A parser configured like production, except for the size limit under test. */
    private static StaxVfrParser parserWithLimit(long maxUncompressedBytes) {
        return new StaxVfrParser(new ImportProperties(
                URI.create("https://www.smartform.cz/download/kopidlno.xml.zip"), List.of("smartform.cz"), false,
                DataSize.ofBytes(maxUncompressedBytes)));
    }

    private static String sampleXml() {
        try (var in = StaxVfrParserTest.class.getResourceAsStream("/vfr/kopidlno-sample.xml")) {
            return new String(in.readAllBytes(), UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static InputStream stream(String xml) {
        return stream(xml.getBytes(UTF_8));
    }

    private static InputStream stream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    /** A stream that delivers {@code prefix} and then fails every read with {@code failure}. */
    private static InputStream failingAfter(byte[] prefix, IOException failure) {
        return new SequenceInputStream(new ByteArrayInputStream(prefix), new InputStream() {
            @Override
            public int read() throws IOException {
                throw failure;
            }
        });
    }

    /** A stream that remembers whether it has been closed. */
    private static final class CloseTrackingInputStream extends ByteArrayInputStream {

        private boolean closed;

        CloseTrackingInputStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static void assertKopidlno(MunicipalityDataset dataset) {
        assertThat(dataset.municipality()).isEqualTo(new Municipality(KOPIDLNO, "Kopidlno"));
        assertThat(dataset.parts()).containsExactlyElementsOf(KOPIDLNO_PARTS);
    }

    @Test
    void parsesPlainXml() {
        assertKopidlno(parser.parse(stream(sampleXml())));
    }

    @Test
    void parsesZippedXml() {
        var zip = TestZips.zip("sample.xml", sampleXml().getBytes(UTF_8));

        assertKopidlno(parser.parse(stream(zip)));
    }

    @Test
    void skipsDirectoriesAndOtherEntriesBeforeTheXmlEntry() {
        var zip = TestZips.zip(List.of(
                new TestZips.Entry("data/", new byte[0]),
                new TestZips.Entry("notes.txt", "not xml".getBytes(UTF_8)),
                new TestZips.Entry("data/sample.XML", sampleXml().getBytes(UTF_8))));

        assertKopidlno(parser.parse(stream(zip)));
    }

    @Test
    void doesNotCloseTheCallerStreamWhenReadingAZip() {
        var zip = new CloseTrackingInputStream(TestZips.zip("sample.xml", sampleXml().getBytes(UTF_8)));

        assertKopidlno(parser.parse(zip));

        assertThat(zip.closed).isFalse();
    }

    @Test
    void rejectsZipWithoutXmlEntry() {
        var zip = TestZips.zip("notes.txt", "not xml".getBytes(UTF_8));

        assertThatExceptionOfType(VfrParseException.class)
                .isThrownBy(() -> parser.parse(stream(zip)))
                .withMessageContaining("no .xml entry");
    }

    @Test
    void rejectsFileWithoutMunicipality() {
        var xml = sampleXml().replaceFirst("(?s)<vf:Obce>.*?</vf:Obce>", "");

        assertThatExceptionOfType(VfrParseException.class)
                .isThrownBy(() -> parser.parse(stream(xml)))
                .withMessageContaining("no municipality");
    }

    @Test
    void rejectsFileWithMoreThanOneMunicipality() {
        var sample = sampleXml();
        var obec = sample.substring(sample.indexOf("<vf:Obec "), sample.indexOf("</vf:Obec>") + "</vf:Obec>".length());
        var xml = sample.replace(obec, obec + obec);

        assertThatExceptionOfType(VfrParseException.class)
                .isThrownBy(() -> parser.parse(stream(xml)))
                .withMessageContaining("more than one municipality");
    }

    @Test
    void rejectsPartOfAnotherMunicipality() {
        var xml = sampleXml().replaceFirst("<coi:Obec>\\s*<obi:Kod>573060", "<coi:Obec><obi:Kod>999999");

        assertThatExceptionOfType(VfrParseException.class)
                .isThrownBy(() -> parser.parse(stream(xml)))
                .withMessageContaining("69299")
                .withMessageContaining("999999");
    }

    @Test
    void rejectsDuplicatePart() {
        var sample = sampleXml();
        var end = "</vf:CastObce>";
        var part = sample.substring(sample.indexOf("<vf:CastObce "), sample.indexOf(end) + end.length());
        var xml = sample.replace(part, part + part);

        assertThatExceptionOfType(VfrParseException.class)
                .isThrownBy(() -> parser.parse(stream(xml)))
                .withMessageContaining("69299");
    }

    @Test
    void doesNotResolveExternalEntities() {
        var xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE vf:VymennyFormat [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <vf:VymennyFormat xmlns:vf="urn:cz:isvs:ruian:schemas:VymennyFormatTypy:v1"
                                  xmlns:obi="urn:cz:isvs:ruian:schemas:ObecIntTypy:v1">
                  <vf:Data>
                    <vf:Obce>
                      <vf:Obec>
                        <obi:Kod>573060</obi:Kod>
                        <obi:Nazev>&xxe;</obi:Nazev>
                      </vf:Obec>
                    </vf:Obce>
                  </vf:Data>
                </vf:VymennyFormat>
                """;

        assertThatExceptionOfType(VfrParseException.class)
                .isThrownBy(() -> parser.parse(stream(xml)))
                .withMessageNotContaining("root:");
    }

    @Test
    void stopsReadingAfterTheMunicipalityParts() {
        var sample = sampleXml();
        var xml = sample.substring(0, sample.indexOf("</vf:CastiObci>") + "</vf:CastiObci>".length())
                + "<vf:Parcely><broken>";

        assertKopidlno(parser.parse(stream(xml)));
    }

    @Test
    void rejectsInputBeyondTheUncompressedLimit() {
        var limitedParser = parserWithLimit(1024);

        assertThatExceptionOfType(VfrParseException.class)
                .isThrownBy(() -> limitedParser.parse(stream(sampleXml())))
                .withMessage("Input exceeds the limit of 1024 bytes")
                .withNoCause();
    }

    @Test
    void reportsMalformedXmlWithItsCause() {
        assertThatExceptionOfType(VfrParseException.class)
                .isThrownBy(() -> parser.parse(stream("<vf:VymennyFormat><vf:Data>")))
                .withMessageStartingWith("Malformed VFR XML: ")
                .withCauseInstanceOf(XMLStreamException.class);
    }

    @Test
    void propagatesTransportFailureOnTheFirstRead() {
        var failure = new IOException("connection reset");

        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> parser.parse(failingAfter(new byte[0], failure)))
                .withCause(failure);
    }

    @Test
    void propagatesTransportFailureInTheMiddleOfTheDocument() {
        var failure = new IOException("connection reset");
        var prefix = sampleXml().substring(0, 2000).getBytes(UTF_8);

        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> parser.parse(failingAfter(prefix, failure)))
                .withCause(failure);
    }
}
