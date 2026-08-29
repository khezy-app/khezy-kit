package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.stream.TextChunker;

import java.util.ArrayList;
import java.util.List;

/**
 * Walks a {@link TextChunker} and applies the two streaming boundary filters that keep per-window
 * detection identical to whole-input detection (design §14.5), assuming every token is no longer
 * than the chunk overlap (design §10.1 — a longer match is out of scope):
 *
 * <ol>
 *   <li><b>Right edge:</b> in a non-final window, a span that ends exactly at the window's end is a
 *       token cut by the boundary; it is dropped because the overlap re-finds the whole token in the
 *       following window (where it is kept and deduplicated).</li>
 *   <li><b>Left edge:</b> in a non-first window, a span that starts at the window's first character
 *       while the previous window ends in a word character (letter, digit, {@code _}) is a fragment
 *       that only looks like a match because the window boundary vacuously opens the match's left
 *       side; the whole token is present (when no longer than the overlap) in the previous window
 *       and is reported from there instead.</li>
 * </ol>
 */
final class WindowedSpanScanner {

    private WindowedSpanScanner() {
    }

    /**
     * Scans every window and returns the kept spans in window order (within a window, in the
     * detector's order). The returned order is the same as the check's in-memory first-seen token
     * order over the full text.
     *
     * @param chunker  the windowed input source
     * @param detector the per-window detector
     * @return the spans that survive the boundary filters
     */
    static List<TokenSpan> scan(final TextChunker chunker,
                                final Detector detector) {
        final var spans = new ArrayList<TokenSpan>();
        var index = 0;
        var prevLastChar = 0;
        var hasPrev = false;
        while (chunker.hasNext()) {
            final var text = chunker.next();
            final var first = index == 0;
            final var last = !chunker.hasNext();
            final var prevChar = hasPrev ? String.valueOf((char) prevLastChar) : "";
            final var prevEndsWithWord = hasPrev && isWordChar((char) prevLastChar);
            final var detected = detector.detect(
                    new WindowMeta(text, index, first, last, prevChar, prevEndsWithWord));
            for (final var span : detected) {
                if (!first && !last && span.end() == text.length()) {
                    continue;
                }
                if (!first && span.start() == 0 && prevEndsWithWord) {
                    continue;
                }
                spans.add(span);
            }
            prevLastChar = text.charAt(text.length() - 1);
            hasPrev = true;
            index++;
        }
        return spans;
    }

    private static boolean isWordChar(final char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}