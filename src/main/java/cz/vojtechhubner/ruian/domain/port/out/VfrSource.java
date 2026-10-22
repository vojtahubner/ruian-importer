package cz.vojtechhubner.ruian.domain.port.out;

import cz.vojtechhubner.ruian.domain.exception.SourceUnavailableException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Provides the VFR XML of a source as a stream, execute-around style.
 */
public interface VfrSource {

    /**
     * Opens {@code source} and hands its VFR XML stream to {@code reader}, returning what the reader produced. The
     * stream is valid only inside {@code reader}: implementations close it afterwards and translate I/O failures,
     * including those thrown by the reader, into {@link SourceUnavailableException}.
     */
    <T> T read(URI source, Reader<T> reader);

    /**
     * Consumes the open stream of a source and produces a result.
     */
    @FunctionalInterface
    interface Reader<T> {

        T read(InputStream in) throws IOException;
    }
}
