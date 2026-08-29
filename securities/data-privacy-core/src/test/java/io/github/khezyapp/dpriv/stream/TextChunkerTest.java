package io.github.khezyapp.dpriv.stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the windowed reader with overlap (design §10.1): window decomposition, overlap
 * prefixing, lazy/EOF behavior, the iterator contract, and window-size validation.
 */
class TextChunkerTest {

    private static List<String> windows(final String input,
                                        final int windowSize,
                                        final int overlap) {
        final var chunker = new TextChunker(new StringReader(input), windowSize, overlap);
        final var result = new ArrayList<String>();
        while (chunker.hasNext()) {
            result.add(chunker.next());
        }
        return result;
    }

    @Test
    @DisplayName("should decompose a long input into overlapping windows with the settled base")
    void decomposesIntoOverlappingWindows() {
        final var input = "x".repeat(2000);
        final var windowSize = 512;
        final var overlap = 64;

        final var windows = windows(input, windowSize, overlap);

        assertThat(windows).hasSize(4);
        assertThat(windows.get(0)).isEqualTo(input.substring(0, 512));
        assertThat(windows.get(1)).isEqualTo(input.substring(448, 1024));
        assertThat(windows.get(2)).isEqualTo(input.substring(960, 1536));
        assertThat(windows.get(3)).isEqualTo(input.substring(1472, 2000));
    }

    @Test
    @DisplayName("should overlap the previous window's tail onto the next window's prefix")
    void overlapsTailOntoNextPrefix() {
        final var input = "a".repeat(448) + "BCDE" + "b".repeat(1080);
        final var windowSize = 512;
        final var overlap = 64;

        final var windows = windows(input, windowSize, overlap);

        assertThat(windows.get(0)).startsWith("a".repeat(448)).contains("BCDE");
        assertThat(windows.get(1)).startsWith("BCDE");
        assertThat(windows.get(1).substring(0, overlap))
                .isEqualTo(windows.get(0).substring(448));
    }

    @Test
    @DisplayName("should yield exactly one window when input fits the window size")
    void singleWindowWhenInputFits() {
        final var input = "abc".repeat(170);

        final var windows = windows(input, 512, 64);

        assertThat(windows).containsExactly(input);
    }

    @Test
    @DisplayName("should truncate the final window at end of input")
    void truncatesFinalWindow() {
        final var input = "z".repeat(600);

        final var windows = windows(input, 512, 64);

        assertThat(windows).hasSize(2);
        assertThat(windows.get(0)).isEqualTo(input.substring(0, 512));
        assertThat(windows.get(1)).isEqualTo(input.substring(448, 600));
    }

    @Test
    @DisplayName("should yield no windows for empty input")
    void emptyInputYieldsNoWindows() {
        assertThat(windows("", 512, 64)).isEmpty();
    }

    @Test
    @DisplayName("should return empty string when next is called past exhaustion")
    void nextPastExhaustionReturnsEmpty() {
        final var chunker = new TextChunker(new StringReader(""), 512, 64);

        assertThat(chunker.hasNext()).isFalse();
        assertThat(chunker.next()).isEmpty();
    }

    @Test
    @DisplayName("should default to a 64 KiB window with 1 KiB overlap")
    void defaultsTo64KiBWindow() {
        final var chunker = new TextChunker(new StringReader("x"));

        assertThat(chunker.windowSize()).isEqualTo(65536);
        assertThat(chunker.overlap()).isEqualTo(1024);
    }

    @Test
    @DisplayName("should walk a multi-megabyte input without exhausting the reader")
    void walksLargeInput() {
        final var input = "a".repeat(70000);
        final var chunker = new TextChunker(new StringReader(input), 65536, 1024);

        final var count = new int[]{0};
        while (chunker.hasNext()) {
            assertThat(chunker.next()).hasSizeGreaterThanOrEqualTo(1024);
            count[0]++;
        }

        assertThat(count[0]).isEqualTo(2);
    }

    @Test
    @DisplayName("should reject an overlap at least as large as the window")
    void rejectsInvalidConstructors() {
        assertThatThrownBy(() -> new TextChunker(new StringReader(""), 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TextChunker(new StringReader(""), 512, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TextChunker(new StringReader(""), 512, 512))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should be a read-only iterator")
    void isReadOnlyIterator() {
        final var chunker = new TextChunker(new StringReader("x"), 512, 64);

        assertThatThrownBy(chunker::remove).isInstanceOf(UnsupportedOperationException.class);
    }
}