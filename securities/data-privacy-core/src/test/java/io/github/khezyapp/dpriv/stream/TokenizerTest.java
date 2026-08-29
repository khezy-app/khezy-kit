package io.github.khezyapp.dpriv.stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the word-run tokenizer used for boundary-safe token walks (design §10.3): the
 * {@code [A-Za-z0-9_]} grammar, Unicode letters, optional truncation, and the maximal trailing
 * boundary extraction.
 */
class TokenizerTest {

    @Test
    @DisplayName("should split a text into maximal word-character runs")
    void splitsIntoWordRuns() {
        assertThat(Tokenizer.tokens("hello world", 0)).containsExactly("hello", "world");
        assertThat(Tokenizer.tokens("  spaced----out_ 123", 0))
                .containsExactly("spaced", "out_", "123");
    }

    @Test
    @DisplayName("should treat empty and punctuation-only text as token free")
    void emptyAndPunctuationOnly() {
        assertThat(Tokenizer.tokens("", 0)).isEmpty();
        assertThat(Tokenizer.tokens("..., !@# $%^", 0)).isEmpty();
    }

    @Test
    @DisplayName("should keep Unicode letters and digits in a single token")
    void unicodeWordsStayWhole() {
        assertThat(Tokenizer.tokens("héllo muno-123", 0)).containsExactly("héllo", "muno", "123");
        assertThat(Tokenizer.tokens("مرحبا", 0)).containsExactly("مرحبا");
    }

    @Test
    @DisplayName("should truncate every token to the configured maximum length")
    void truncatesTokens() {
        assertThat(Tokenizer.tokens("abcdef x", 3)).containsExactly("abc", "x");
        assertThat(Tokenizer.tokens("a b c", 5)).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("should leave tokens unlimited when maxLen is zero")
    void zeroMaxLenMeansUnlimited() {
        final var longWord = "x".repeat(10_000);

        assertThat(Tokenizer.tokens(longWord, 0)).containsExactly(longWord);
    }

    @Test
    @DisplayName("should extract the maximal trailing word run")
    void extractsTrailingBoundary() {
        assertThat(Tokenizer.trailingBoundary("ab cd!ef")).isEqualTo("ef");
        assertThat(Tokenizer.trailingBoundary("plain")).isEqualTo("plain");
        assertThat(Tokenizer.trailingBoundary("ab1_2")).isEqualTo("ab1_2");
        assertThat(Tokenizer.trailingBoundary("ab ")).isEmpty();
        assertThat(Tokenizer.trailingBoundary("")).isEmpty();
    }
}