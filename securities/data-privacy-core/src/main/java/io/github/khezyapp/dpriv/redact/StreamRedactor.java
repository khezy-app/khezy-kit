package io.github.khezyapp.dpriv.redact;

import io.github.khezyapp.dpriv.internal.AhoCorasick;
import io.github.khezyapp.dpriv.stream.TextChunker;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.github.khezyapp.dpriv.redact.MatchSelection.Match;

/**
 * Streaming, single-pass redactor (design §7.2/§7.3). Consumes a {@link TextChunker} window by
 * window, scans each window with the same Aho-Corasick automaton over the same token set the
 * in-memory {@link Redactor} uses, and writes the redacted text to a {@link Writer} without ever
 * materializing the whole input. Peak memory is bounded by one window plus the seen-token set
 * (design §10.2).
 *
 * <p>Matches are committed with a <b>safe-commit rule</b>: in a non-final window, nothing is
 * written past {@code boundary - overlap}, because every match that is not visible yet (it
 * straddles the boundary, or lies inside the overlap tail) must — assuming no token is longer than
 * the overlap — start inside the last {@code overlap} characters of the window. Writing past a
 * match's start before it is fully known could split a placeholder or strand a longer match that
 * subsumes an earlier emitted shorter one (e.g. a boundary-straddling URL containing a shorter
 * token). The whole uncertain tail is therefore deferred: only matches ending at or before the cut
 * are written, and the raw text after the cut is held back and re-scanned in the following window,
 * where the matches reappear at their true absolute offsets and the same longest-first selection
 * applies. Because the overlap is scanned twice, spans whose absolute end is already committed
 * ({@code end <= flushOffset}) are skipped on re-detection. A rarely reached buffer back-fill keeps
 * the last {@code 2 * overlap} characters of the previous window so a held match that starts below
 * the next window's base can still be emitted at its true position. The final window commits
 * everything. With this rule, streaming output is character-identical to
 * {@code Redactor.redact(fullText, tokens)} — the parity contract asserted by Task 09. Matches
 * never overlap across windows, so no inter-window protection set is needed.
 *
 * <p>An empty {@code maskEntities} is allowed and yields a pure pass-through copy of the input.
 */
public final class StreamRedactor {

    private final TextChunker chunker;
    private final AhoCorasick automaton;
    private final Set<Match> pending = new LinkedHashSet<>();
    private final StringBuilder buffer = new StringBuilder();
    private String prevTail = "";
    private int prevTailStart;
    private int flushOffset;

    /**
     * Creates a streaming redactor over the given chunked input and token map.
     *
     * @param chunker      the windowed input source; never null
     * @param maskEntities {@code entityType → tokens}, keyed by policy rule name; may be empty for
     *                     pass-through
     */
    public StreamRedactor(final TextChunker chunker,
                          final Map<String, List<String>> maskEntities) {
        this.chunker = Objects.requireNonNull(chunker, "chunker");
        Objects.requireNonNull(maskEntities, "maskEntities");
        this.automaton = maskEntities.isEmpty() ? null : AhoCorasick.compile(maskEntities);
    }

    /**
     * Streams the redaction of the chunker's input, writing to {@code out}. The writer is not
     * closed or flushed; the caller owns its lifecycle.
     *
     * @param out the writer receiving the redacted text; never null
     * @throws IOException if writing fails
     */
    public void redact(final Writer out) throws IOException {
        Objects.requireNonNull(out, "out");
        var windowIndex = 0;
        while (chunker.hasNext()) {
            final var window = chunker.next();
            final var last = !chunker.hasNext();
            final var base = windowIndex == 0
                    ? 0
                    : windowIndex * chunker.windowSize() - chunker.overlap();
            final var boundary = base + window.length();
            scan(window, base);
            emitWindow(out, window, base, boundary, last);
            windowIndex++;
        }
    }

    private void scan(final String window,
                      final int base) {
        if (Objects.isNull(automaton)) {
            return;
        }
        automaton.scan(window, (start, end, token, entityType) ->
                pending.add(new Match(base + start, base + end, token, entityType)));
    }

    private void emitWindow(final Writer out,
                            final String window,
                            final int base,
                            final int boundary,
                            final boolean last) throws IOException {
        buffer.setLength(0);
        if (flushOffset < base) {
            buffer.append(prevTail, flushOffset - prevTailStart, base - prevTailStart);
            buffer.append(window, 0, window.length());
        } else {
            buffer.append(window, flushOffset - base, window.length());
        }

        final var limit = last ? boundary : boundary - chunker.overlap();
        final var selected = MatchSelection.selectLongestFirst(new ArrayList<>(pending));

        var emitEnd = flushOffset;
        var holdStart = limit;
        for (final var span : selected) {
            if (span.end() <= flushOffset) {
                continue;
            }
            if (span.end() > limit) {
                holdStart = Math.min(holdStart, span.start());
                continue;
            }
            writeRaw(out, emitEnd, span.start());
            out.write(Placeholders.forEntityType(span.entityType()));
            emitEnd = span.end();
        }
        writeRaw(out, emitEnd, holdStart);
        flushOffset = holdStart;

        final var keepFrom = Math.max(base, boundary - 2 * chunker.overlap());
        prevTail = window.substring(keepFrom - base);
        prevTailStart = keepFrom;

        buffer.setLength(0);
        pending.removeIf(span -> span.end() <= flushOffset);
    }

    private void writeRaw(final Writer out,
                          final int start,
                          final int end) throws IOException {
        if (start >= end) {
            return;
        }
        out.write(buffer.substring(start - flushOffset, end - flushOffset));
    }
}