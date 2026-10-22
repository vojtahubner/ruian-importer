package cz.vojtechhubner.ruian.adapter.out.xml;

import static cz.vojtechhubner.ruian.adapter.out.xml.VfrNamespaces.COI;
import static cz.vojtechhubner.ruian.adapter.out.xml.VfrNamespaces.OBI;
import static cz.vojtechhubner.ruian.adapter.out.xml.VfrNamespaces.VF;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import cz.vojtechhubner.ruian.application.ImportProperties;
import cz.vojtechhubner.ruian.domain.exception.VfrParseException;
import cz.vojtechhubner.ruian.domain.model.Municipality;
import cz.vojtechhubner.ruian.domain.model.MunicipalityDataset;
import cz.vojtechhubner.ruian.domain.model.MunicipalityPart;
import cz.vojtechhubner.ruian.domain.port.out.VfrParser;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Reads the municipality and its parts out of a VFR document, plain or inside a ZIP archive, with the StAX cursor
 * API, so that a document of any size is processed in constant memory and reading stops as soon as the
 * {@code vf:CastiObci} section is over.
 * <p>
 * Failures are translated as follows: an {@link XMLStreamException} becomes a {@link VfrParseException}
 * ("Malformed VFR XML: ...") unless an {@link IOException} is found in its cause chain, which means the transport
 * failed and is rethrown as an {@link UncheckedIOException}; a {@link ZipException} becomes a
 * {@link VfrParseException} ("Invalid ZIP archive: ...") and any other {@link IOException} an
 * {@link UncheckedIOException}; a {@link NumberFormatException} and an {@link IllegalArgumentException} from the
 * domain records become a {@link VfrParseException} with the same message; the {@link VfrParseException} raised by
 * the size limit passes through unchanged. The ZIP and XML readers created here are closed before returning; the
 * caller owns the stream it passes in, which is neither closed nor read past the point where parsing stopped.
 */
@Component
public class StaxVfrParser implements VfrParser {

    private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04};

    private final long maxUncompressedBytes;
    private final XMLInputFactory factory;

    public StaxVfrParser(ImportProperties properties) {
        this(properties.maxUncompressedBytes().toBytes());
    }

    StaxVfrParser(long maxUncompressedBytes) {
        this.maxUncompressedBytes = maxUncompressedBytes;
        this.factory = XMLInputFactory.newDefaultFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
    }

    @Override
    public MunicipalityDataset parse(InputStream in) {
        try {
            var pushback = new PushbackInputStream(in, ZIP_MAGIC.length);
            if (!isZip(pushback)) {
                return parseXml(pushback);
            }
            try (var zip = new ZipInputStream(new CloseShieldInputStream(pushback))) {
                return parseXml(firstXmlEntry(zip));
            }
        } catch (ZipException e) {
            throw new VfrParseException("Invalid ZIP archive: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Tells a ZIP archive from plain XML by its first four bytes, which are pushed back for the actual reader. */
    private static boolean isZip(PushbackInputStream in) throws IOException {
        var header = new byte[ZIP_MAGIC.length];
        int read = in.readNBytes(header, 0, header.length);
        in.unread(header, 0, read);
        return read == header.length && Arrays.equals(header, ZIP_MAGIC);
    }

    /** Positions the archive on its first {@code .xml} entry; entry sizes are not trusted, the bytes are counted. */
    private static InputStream firstXmlEntry(ZipInputStream zip) throws IOException {
        for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            if (!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).endsWith(".xml")) {
                return zip;
            }
        }
        throw new VfrParseException("ZIP archive contains no .xml entry");
    }

    private MunicipalityDataset parseXml(InputStream xml) {
        try {
            var reader = factory.createXMLStreamReader(new BoundedInputStream(xml, maxUncompressedBytes));
            try {
                return readDataset(reader);
            } finally {
                reader.close();
            }
        } catch (XMLStreamException e) {
            throw translate(e);
        } catch (IllegalArgumentException e) {
            throw new VfrParseException(e.getMessage(), e);
        }
    }

    private static MunicipalityDataset readDataset(XMLStreamReader reader) throws XMLStreamException {
        Municipality municipality = null;
        var parts = new ArrayList<MunicipalityPart>();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == START_ELEMENT && isElement(reader, VF, "Obec")) {
                if (municipality != null) {
                    throw new VfrParseException("VFR file contains more than one municipality");
                }
                municipality = readMunicipality(reader);
            } else if (event == START_ELEMENT && isElement(reader, VF, "CastObce")) {
                parts.add(readPart(reader));
            } else if (event == END_ELEMENT && (isElement(reader, VF, "CastiObci") || isElement(reader, VF, "Data"))) {
                break;
            }
        }
        if (municipality == null) {
            throw new VfrParseException("VFR file contains no municipality");
        }
        for (var part : parts) {
            if (part.municipalityCode() != municipality.code()) {
                throw new VfrParseException("Part " + part.code() + " belongs to municipality "
                        + part.municipalityCode() + ", expected " + municipality.code());
            }
        }
        return new MunicipalityDataset(municipality, parts);
    }

    /** Reads the children of the {@code vf:Obec} element the reader is positioned on. */
    private static Municipality readMunicipality(XMLStreamReader reader) throws XMLStreamException {
        Integer code = null;
        String name = null;
        for (int event = reader.next(); event != END_ELEMENT; event = reader.next()) {
            if (event != START_ELEMENT) {
                continue;
            }
            if (isElement(reader, OBI, "Kod")) {
                code = readInt(reader);
            } else if (isElement(reader, OBI, "Nazev")) {
                name = reader.getElementText();
            } else {
                skipElement(reader);
            }
        }
        if (code == null || name == null) {
            throw new VfrParseException("Municipality element lacks Kod or Nazev");
        }
        return new Municipality(code, name);
    }

    /** Reads the children of the {@code vf:CastObce} element the reader is positioned on. */
    private static MunicipalityPart readPart(XMLStreamReader reader) throws XMLStreamException {
        Integer code = null;
        String name = null;
        Integer municipalityCode = null;
        for (int event = reader.next(); event != END_ELEMENT; event = reader.next()) {
            if (event != START_ELEMENT) {
                continue;
            }
            if (isElement(reader, COI, "Kod")) {
                code = readInt(reader);
            } else if (isElement(reader, COI, "Nazev")) {
                name = reader.getElementText();
            } else if (isElement(reader, COI, "Obec")) {
                municipalityCode = readMunicipalityCode(reader);
            } else {
                skipElement(reader);
            }
        }
        if (code == null || name == null || municipalityCode == null) {
            throw new VfrParseException("Municipality part element lacks Kod, Nazev or Obec/Kod");
        }
        return new MunicipalityPart(code, name, municipalityCode);
    }

    /** Reads the {@code obi:Kod} child of the {@code coi:Obec} element the reader is positioned on. */
    private static @Nullable Integer readMunicipalityCode(XMLStreamReader reader) throws XMLStreamException {
        Integer code = null;
        for (int event = reader.next(); event != END_ELEMENT; event = reader.next()) {
            if (event != START_ELEMENT) {
                continue;
            }
            if (isElement(reader, OBI, "Kod")) {
                code = readInt(reader);
            } else {
                skipElement(reader);
            }
        }
        return code;
    }

    private static int readInt(XMLStreamReader reader) throws XMLStreamException {
        return Integer.parseInt(reader.getElementText().trim());
    }

    /** Consumes the element the reader is positioned on together with everything nested in it. */
    private static void skipElement(XMLStreamReader reader) throws XMLStreamException {
        int depth = 1;
        while (depth > 0) {
            int event = reader.next();
            if (event == START_ELEMENT) {
                depth++;
            } else if (event == END_ELEMENT) {
                depth--;
            }
        }
    }

    private static boolean isElement(XMLStreamReader reader, String namespace, String localName) {
        return localName.equals(reader.getLocalName()) && namespace.equals(reader.getNamespaceURI());
    }

    private static RuntimeException translate(XMLStreamException e) {
        for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof IOException io) {
                return new UncheckedIOException(io);
            }
        }
        return new VfrParseException("Malformed VFR XML: " + e.getMessage(), e);
    }

    /** Lets the ZIP reader be closed, releasing its inflater, without closing the caller's stream underneath it. */
    private static final class CloseShieldInputStream extends FilterInputStream {

        CloseShieldInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() {
            // the caller owns the wrapped stream
        }
    }
}
