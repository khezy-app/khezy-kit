package io.github.khezyapp.dpriv.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Splits a window into maximal runs of word characters ({@code [A-Za-z0-9]}). Matches the grammar
 * a downstream Aho-Corasick scan is seeded with for boundary-safe token re-detection (design
 * §10.3): each returned token is what a projected sub-window's placeholder would look like if the
 * window produced no matches, letting streaming consumers walk to the next token boundary without
 * splitting a token.
 */
public final class Tokenizer {

    private Tokenizer() {
    }

    /**
     * The maximal trailing run of {@code [A-Za-z0-9]} characters of {@code text}.
     *
     * @param text the window tail; never null
     * @return the trailing boundary text (empty when {@code text} ends in a non-word character)
     */
    public static String trailingBoundary(final CharSequence text) {
        final var run = new StringBuilder();
        for (var i = text.length() - 1; i >= 0; i--) {
            if (!isWordChar(text.charAt(i))) {
                break;
            }
            run.append(text.charAt(i));
        }
        run.reverse();
        return run.toString();
    }

    /**
     * Extracts consecutive tokens from {@code text}, each truncated to at most {@code maxLen}
     * characters. A {@code maxLen} of {@code 0} means no truncation.
     *
     * @param text   the window to tokenize; never null
     * @param maxLen the maximum token length (token grammar is {@code [A-Za-z0-9]+})
     * @return the tokens in order of appearance
     */
    public static List<String> tokens(final CharSequence text, final int maxLen) {
        Objects.requireNonNull(text, "text");
        final var result = new ArrayList<String>();
        final var current = new StringBuilder();
        final Runnable flush = () -> {
            if (current.length() == 0) {
                return;
            }
            final var token = current.toString();
            result.add(maxLen > 0 && token.length() > maxLen ? token.substring(0, maxLen) : token);
            current.setLength(0);
        };
        for (var i = 0; i < text.length(); i++) {
            final var c = text.charAt(i);
            if (isWordChar(c)) {
                current.append(c);
            } else {
                flush.run();
            }
        }
        flush.run();
        return result;
    }

    private static boolean isWordChar(final char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}