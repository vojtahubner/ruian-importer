package cz.vojtechhubner.ruian.adapter.out.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds small ZIP archives in memory for parser tests.
 */
final class TestZips {

    /** One archive entry; a name ending with {@code /} denotes a directory. */
    record Entry(String name, byte[] content) {
    }

    private TestZips() {
    }

    static byte[] zip(String entryName, byte[] content) {
        return zip(List.of(new Entry(entryName, content)));
    }

    static byte[] zip(List<Entry> entries) {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            for (var entry : entries) {
                zip.putNextEntry(new ZipEntry(entry.name()));
                zip.write(entry.content());
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }
}
