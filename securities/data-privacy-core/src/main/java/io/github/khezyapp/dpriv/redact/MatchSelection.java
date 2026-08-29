package io.github.khezyapp.dpriv.redact;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shared longest-first selection over matched spans (design §7.2), used by both the in-memory
 * {@link Redactor} and the streaming {@link StreamRedactor} so they pick the identical set of
 * tokens to replace. Selection sorts the given list in place (longest token first, then position,
 * then entity) and keeps every match that does not overlap an already-kept span.
 */
final class MatchSelection {

    private static final Comparator<Match> LONGEST_FIRST =
            Comparator.comparingInt(Match::length)
                    .reversed()
                    .thenComparingInt(Match::start)
                    .thenComparingInt(Match::end)
                    .thenComparing(Match::entityType);

    /**
     * A matched token span with its entity type.
     *
     * @param start      the inclusive start offset in the scanned text
     * @param end        the exclusive end offset in the scanned text
     * @param token      the matched token text
     * @param entityType the entity type the token belongs to
     */
    record Match(int start, int end, String token, String entityType) {

        /**
         * The token length, used for the longest-first ordering.
         *
         * @return the token length
         */
        private int length() {
            return token.length();
        }
    }

    private MatchSelection() {
    }

    /**
     * Longest-first selection with no external context (the in-memory {@link Redactor} path):
     * drops any match overlapping a longer (or earlier) kept match and returns the kept spans
     * sorted by start.
     *
     * @param matches the detected matches, in any order
     * @return the non-overlapping kept matches, sorted by start, in ascending start order
     */
    static List<Match> selectLongestFirst(final List<Match> matches) {
        return selectLongestFirst(matches, List.of());
    }

    /**
     * Longest-first selection that also treats {@code priorProtected} spans as already-kept (the
     * streaming {@link StreamRedactor} path, which commits matches window by window). A match
     * overlapping any previously emitted span is dropped, exactly as if the two had been selected
     * together.
     *
     * <p>The input list is sorted in place by this call; pass a copy when the caller still needs
     * the original order (e.g. {@link StreamRedactor}'s pending buffer).
     *
     * @param matches        the detected matches, in any order
     * @param priorProtected previously kept/emitted spans that must not be re-covered
     * @return the non-overlapping kept matches, sorted by start, in ascending start order
     */
    static List<Match> selectLongestFirst(final List<Match> matches,
                                          final List<Match> priorProtected) {
        matches.sort(LONGEST_FIRST);
        final var kept = new ArrayList<Match>();
        final var protectedSpans = new ArrayList<Match>(priorProtected);
        for (final var match : matches) {
            if (!overlapsProtected(match, protectedSpans)) {
                kept.add(match);
                protectedSpans.add(match);
            }
        }
        kept.sort(Comparator.comparingInt(Match::start));
        return kept;
    }

    private static boolean overlapsProtected(final Match candidate,
                                             final List<Match> protectedSpans) {
        for (final var span : protectedSpans) {
            if (candidate.start() < span.end() && candidate.end() > span.start()) {
                return true;
            }
        }
        return false;
    }
}