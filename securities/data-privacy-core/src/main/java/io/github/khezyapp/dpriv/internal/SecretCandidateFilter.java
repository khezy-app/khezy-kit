package io.github.khezyapp.dpriv.internal;

import io.github.khezyapp.dpriv.policy.SecretPresetParams;

import java.util.Objects;

/**
 * The single source of truth for "is this string a high-entropy secret?" (design §9.2). Held by a
 * preset's {@link SecretPresetParams}, it is a stateless predicate so the in-memory
 * {@code SecretKeysCheck} and the streaming engine (Task 09) share exactly the same decision
 * procedure — they cannot drift.
 *
 * <p>The intrinsic checks run cheapest-first: length, then character diversity, then Shannon
 * entropy (design §9.2 order). When the preset is in strict mode, the context-aware overload
 * additionally requires the candidate to be a maximal run — i.e. not directly glued to another
 * identifier character (letter, digit, or {@code _}) in the surrounding text.
 */
public final class SecretCandidateFilter {

    private static final double LOG2 = Math.log(2.0);

    private final SecretPresetParams params;

    /**
     * Creates a filter bound to the given preset parameters.
     *
     * @param params the resolved preset tuple; never null
     */
    public SecretCandidateFilter(final SecretPresetParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    /**
     * Applies the intrinsic checks only: length, diversity, then entropy. Does not consider the
     * candidate's context, so it is safe to call on a bare candidate string.
     *
     * @param candidate the candidate run; never null
     * @return {@code true} if it satisfies length, diversity and entropy
     */
    public boolean accept(final String candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (candidate.length() < params.minLength()) {
            return false;
        }
        if (distinctCount(candidate) < params.minDiversity()) {
            return false;
        }
        return entropy(candidate) >= params.minEntropy();
    }

    /**
     * Context-aware decision used by checks that know where the candidate sits in the input.
     * Applies the intrinsic checks, then — only in strict mode — requires the token to not be
     * directly adjacent to another identifier character (letter, digit, or {@code _}).
     *
     * @param input the full text being scanned; never null
     * @param start the inclusive start offset of the candidate in {@code input}
     * @param end   the exclusive end offset of the candidate in {@code input}
     * @return {@code true} if the candidate is a valid secret for the preset
     */
    public boolean accept(final String input, final int start, final int end) {
        Objects.requireNonNull(input, "input");
        final var candidate = input.substring(start, end);
        if (!accept(candidate)) {
            return false;
        }
        return !params.strictMode() || isBoundarySafe(input, start, end);
    }

    private static boolean isBoundarySafe(final String input,
                                          final int start,
                                          final int end) {
        if (start > 0 && isIdentifierChar(input.charAt(start - 1))) {
            return false;
        }
        return end >= input.length() || !isIdentifierChar(input.charAt(end));
    }

    private static boolean isIdentifierChar(final char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static int distinctCount(final String candidate) {
        final var seen = new boolean[65536];
        var count = 0;
        for (var i = 0; i < candidate.length(); i++) {
            final var c = candidate.charAt(i);
            if (!seen[c]) {
                seen[c] = true;
                count++;
            }
        }
        return count;
    }

    private static double entropy(final String candidate) {
        final var counts = new int[65536];
        for (var i = 0; i < candidate.length(); i++) {
            counts[candidate.charAt(i)]++;
        }
        final var length = candidate.length();
        var h = 0.0;
        for (final var count : counts) {
            if (count > 0) {
                final var p = (double) count / length;
                h -= p * (Math.log(p) / LOG2);
            }
        }
        return h;
    }
}
