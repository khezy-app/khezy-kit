package io.github.khezyapp.dpriv.redact;

import io.github.khezyapp.dpriv.stream.TextChunker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the streaming single-pass redactor (design §7.2/§7.3): output is character-identical
 * to the in-memory {@link Redactor} for large inputs over every chunk configuration, including
 * tokens that end exactly on or straddle a window boundary. Also covers the pass-through contract
 * for an empty mask.
 */
class StreamRedactorTest {

    private static final String EMAIL = "visal@example.com";
    private static final String SECRET = "gpk3Kd0QxZ9mN4";
    private static final String URL = "https://phnompenh.example.org/path?q=1";
    private static final String KEYWORD = "urgent";

    private static Map<String, List<String>> fixtureMask() {
        final var mask = new LinkedHashMap<String, List<String>>();
        mask.put("pii_email_address", List.of(EMAIL));
        mask.put("secret", List.of("example", SECRET));
        mask.put("link", List.of(URL));
        mask.put("keyword", List.of(KEYWORD));
        return mask;
    }

    private static String repeatedFixture(final int targetLength) {
        final var filler = "lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do "
                + "eiusmod tempor incididunt ut labore et dolore magna aliqua. ";
        final var sb = new StringBuilder(targetLength + 256);
        while (sb.length() < targetLength) {
            sb.append(filler);
            sb.append(EMAIL).append(" or ").append(KEYWORD).append(" details at ")
                    .append(URL).append(" key ").append(SECRET).append(" now\n");
        }
        return sb.toString();
    }

    private static String streamed(final String input,
                                   final int windowSize,
                                   final int overlap,
                                   final Map<String, List<String>> mask) throws Exception {
        final var chunker = new TextChunker(new StringReader(input), windowSize, overlap);
        final var out = new StringWriter();
        new StreamRedactor(chunker, mask).redact(out);
        return out.toString();
    }

    private static String streamedDefault(final String input,
                                          final Map<String, List<String>> mask) throws Exception {
        final var out = new StringWriter();
        new StreamRedactor(new TextChunker(new StringReader(input)), mask).redact(out);
        return out.toString();
    }

    @Test
    @DisplayName("should match the in-memory redactor over a 400 KiB fixture with a 512/64 chunker")
    void parityWithInMemoryOverLargeFixture() throws Exception {
        final String input = repeatedFixture(400_000);
        final var mask = fixtureMask();

        final var expected = new Redactor().redact(input, mask);
        final var actual = streamed(input, 512, 64, mask);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("should match the in-memory redactor with the default 64 KiB chunker")
    void parityWithInMemoryUsingDefaultChunker() throws Exception {
        final String input = repeatedFixture(400_000);
        final var mask = fixtureMask();

        final var actual = streamedDefault(input, mask);

        assertThat(actual).isEqualTo(new Redactor().redact(input, mask));
    }

    @Test
    @DisplayName("should mask a secret token that straddles the default 64 KiB boundary")
    void masksTokenStraddlingDefaultBoundary() throws Exception {
        final var token = "A1b2C3d4E5f6G7h8A1b2C3d4E5f6G7h8A1b2C3d4E5f6G7h8A1b2C3d4E5f6G7h8A1b2C3d4E5";
        final var mask = Map.of("secret", List.of(token));
        final var input = "x".repeat(65501) + token + " tail";

        final var actual = streamedDefault(input, mask);
        final var expected = new Redactor().redact(input, mask);

        assertThat(actual).isEqualTo(expected);
        assertThat(actual).doesNotContain(token);
        assertThat(actual).startsWith("x".repeat(65501) + "<SECRET>");
    }

    @Test
    @DisplayName("should mask a token that ends exactly on the default 64 KiB boundary")
    void masksTokenEndingOnDefaultBoundary() throws Exception {
        final var mask = Map.of("pii_email_address", List.of(EMAIL));
        final var input = "x".repeat(65536 - EMAIL.length()) + EMAIL + " after";

        final var actual = streamedDefault(input, mask);
        final var expected = new Redactor().redact(input, mask);

        assertThat(actual).isEqualTo(expected);
        assertThat(actual).startsWith("x".repeat(65536 - EMAIL.length()) + "<EMAIL_ADDRESS>");
    }

    @Test
    @DisplayName("should pass the input through unchanged when the mask is empty")
    void passesThroughWhenMaskIsEmpty() throws Exception {
        final String input = repeatedFixture(60_000);

        assertThat(streamed(input, 512, 64, Map.of())).isEqualTo(input);
    }

    @Test
    @DisplayName("should leave token-free text untouched")
    void leavesCleanTextUntouched() throws Exception {
        final String input = "no sensitive content in this short paragraph.";

        final var actual = streamedDefault(input, fixtureMask());

        assertThat(actual).isEqualTo(input);
    }

    @Test
    @DisplayName("should keep the longer token over a contained shorter token mid-window")
    void longestTokenWinsAcrossBoundaries() throws Exception {
        final var mask = new LinkedHashMap<String, List<String>>();
        mask.put("pii_email_address", List.of(EMAIL));
        mask.put("secret", List.of("example"));
        final var input = "prefix " + EMAIL + " and a standalone example token";

        final var actual = streamed(input, 512, 64, mask);
        final var expected = new Redactor().redact(input, mask);

        assertThat(actual).isEqualTo(expected);
        assertThat(actual).isEqualTo("prefix <EMAIL_ADDRESS> and a standalone <SECRET> token");
    }
}