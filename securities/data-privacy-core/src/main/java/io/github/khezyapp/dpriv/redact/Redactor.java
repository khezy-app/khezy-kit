package io.github.khezyapp.dpriv.redact;

import io.github.khezyapp.dpriv.internal.AhoCorasick;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.github.khezyapp.dpriv.redact.MatchSelection.Match;

/**
 * In-memory, whole-input redactor (design §7.2 — "literal longest-first"). Every occurrence of
 * every token in {@code maskEntities} is replaced with {@link Placeholders#forEntityType}. When a
 * token is a substring of another, the longer wins and the region it covers is consumed (the
 * shorter never applies inside its span); a placeholder is never matched inside another emitted
 * placeholder. Redaction is pure and deterministic: the input is never mutated.
 *
 * <p>The streaming {@code StreamRedactor} (Task 09) must produce byte-identical output for the same
 * tokens — the parity contract asserted in Task 09.
 */
public final class Redactor {

    /**
     * Replaces every token occurrence in {@code input} with its {@code <ENTITY>} placeholder.
     *
     * @param input        the text to redact; never null
     * @param maskEntities {@code entityType → tokens}, keyed by policy rule name
     * @return the redacted text
     * @throws NullPointerException if {@code input} or {@code maskEntities} is null
     */
    public String redact(final String input,
                         final Map<String, List<String>> maskEntities) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(maskEntities, "maskEntities");
        if (maskEntities.isEmpty()) {
            return input;
        }
        final var matches = collectMatches(input, maskEntities);
        if (matches.isEmpty()) {
            return input;
        }
        final var kept = MatchSelection.selectLongestFirst(matches);
        return apply(input, kept);
    }

    private static List<Match> collectMatches(final String input,
                                              final Map<String, List<String>> maskEntities) {
        final var automaton = AhoCorasick.compile(maskEntities);
        final var matches = new ArrayList<Match>();
        automaton.scan(input, (start, end, token, entityType) ->
                matches.add(new Match(start, end, token, entityType)));
        return matches;
    }

    private static String apply(final String input,
                                final List<Match> kept) {
        final var sb = new StringBuilder(input.length());
        var cursor = 0;
        for (final var match : kept) {
            if (match.start() < cursor) {
                continue;
            }
            sb.append(input, cursor, match.start());
            sb.append(Placeholders.forEntityType(match.entityType()));
            cursor = match.end();
        }
        sb.append(input, cursor, input.length());
        return sb.toString();
    }
}