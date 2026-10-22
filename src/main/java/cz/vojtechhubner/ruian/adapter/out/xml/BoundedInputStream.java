package cz.vojtechhubner.ruian.adapter.out.xml;

import cz.vojtechhubner.ruian.domain.exception.VfrParseException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Counts the bytes delivered by the wrapped stream and fails as soon as they exceed a limit, so that a decompressed
 * document cannot grow without bound.
 */
final class BoundedInputStream extends FilterInputStream {

    private final long maxBytes;
    private long delivered;

    BoundedInputStream(InputStream in, long maxBytes) {
        super(in);
        this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value != -1) {
            count(1);
        }
        return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int read = super.read(buffer, offset, length);
        if (read > 0) {
            count(read);
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = super.skip(n);
        count(skipped);
        return skipped;
    }

    private void count(long bytes) {
        delivered += bytes;
        if (delivered > maxBytes) {
            throw new VfrParseException("Input exceeds the limit of " + maxBytes + " bytes");
        }
    }
}
