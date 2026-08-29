package io.github.khezyapp.dpriv.stream;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Objects;

/**
 * Windowed reader with overlap (design §10.1). Reads the {@link Reader} lazily in fixed-size
 * windows of {@code windowSize} characters, retaining the last {@code overlap} characters as the
 * next window's prefix so a match that straddles a chunk boundary is found whole in the following
 * window. The final window is whatever remains and may be shorter. Empty input yields no windows.
 *
 * <p>Total windows never exceed {@code reader.length / (windowSize - overlap) + 1}. The overlap
 * must be strictly smaller than the window size (IllegalArgumentException otherwise); the caller
 * must also keep {@code overlap} larger than the longest token a downstream consumer can emit so
 * boundary-crossing matches are always re-detected whole (defaults: 64 KB window, 1 KB overlap).
 *
 * <p>{@code next()} returns an empty string once iteration is exhausted instead of throwing, and
 * {@link IOException}s from the underlying reader surface as {@link UncheckedIOException}.
 */
public final class TextChunker implements Iterator<String> {

    private static final int EOF = -1;

    private final Reader reader;
    private final int windowSize;
    private final int overlap;
    private final char[] buffer;

    private String tail = "";
    private String nextWindow;
    private boolean finished;

    /**
     * Creates a chunker with the default 64 KB window and 1 KB overlap.
     *
     * @param reader the source to window; never null
     */
    public TextChunker(final Reader reader) {
        this(reader, 65536, 1024);
    }

    /**
     * Creates a chunker with explicit window and overlap sizes.
     *
     * @param reader     the source to window; never null
     * @param windowSize the window size in characters; must be positive
     * @param overlap    the overlap carried across windows; must be non-negative and smaller than
     *                   {@code windowSize}
     * @throws IllegalArgumentException if {@code windowSize <= 0} or {@code overlap < 0} or
     *                                  {@code overlap >= windowSize}
     */
    public TextChunker(final Reader reader,
                       final int windowSize,
                       final int overlap) {
        this.reader = Objects.requireNonNull(reader, "reader");
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be positive");
        }
        if (overlap < 0) {
            throw new IllegalArgumentException("overlap must be non-negative");
        }
        if (overlap >= windowSize) {
            throw new IllegalArgumentException("overlap must be smaller than windowSize");
        }
        this.windowSize = windowSize;
        this.overlap = overlap;
        this.buffer = new char[windowSize];
    }

    /**
     * The configured window size.
     *
     * @return the window size in characters
     */
    public int windowSize() {
        return windowSize;
    }

    /**
     * The configured overlap.
     *
     * @return the overlap in characters
     */
    public int overlap() {
        return overlap;
    }

    @Override
    public boolean hasNext() {
        if (Objects.nonNull(nextWindow)) {
            return true;
        }
        if (finished) {
            return false;
        }
        nextWindow = readWindow();
        return Objects.nonNull(nextWindow);
    }

    @Override
    public String next() {
        if (Objects.isNull(nextWindow)) {
            final var freshlyRead = hasNext();
            if (!freshlyRead) {
                return "";
            }
        }
        final var result = nextWindow;
        nextWindow = null;
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("TextChunker does not support removal");
    }

    private String readWindow() {
        final var fresh = readChunk();
        if (Objects.isNull(fresh)) {
            finished = true;
            tail = "";
            return null;
        }
        final var window = tail + fresh;
        if (fresh.length() < windowSize) {
            finished = true;
            tail = "";
            return window;
        }
        tail = window.length() > overlap
                ? window.substring(window.length() - overlap)
                : window;
        return window;
    }

    private String readChunk() {
        final var total = fill();
        if (total == 0) {
            return null;
        }
        return new String(buffer, 0, total);
    }

    private int fill() {
        var total = 0;
        while (total < windowSize) {
            final int count;
            try {
                count = reader.read(buffer, total, windowSize - total);
            } catch (final IOException e) {
                throw new UncheckedIOException("failed to read text window from reader", e);
            }
            if (count == EOF || count == 0) {
                break;
            }
            total += count;
        }
        return total;
    }
}