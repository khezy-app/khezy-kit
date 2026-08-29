package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.CustomRegexConfig;
import io.github.khezyapp.dpriv.api.GuardrailCheck;
import io.github.khezyapp.dpriv.api.KeywordsConfig;
import io.github.khezyapp.dpriv.api.PiiConfig;
import io.github.khezyapp.dpriv.api.PiiCoverage;
import io.github.khezyapp.dpriv.api.SecretConfig;
import io.github.khezyapp.dpriv.api.UrlsConfig;
import io.github.khezyapp.dpriv.policy.PiiEntity;
import io.github.khezyapp.dpriv.redact.Redactor;
import io.github.khezyapp.dpriv.stream.MatchAccumulator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Task 09 parity contract (design §14.5): for every check, the streaming
 * {@code toStream().scan(...)} output — entity keys and per-key first-seen token lists — is
 * identical to the in-memory {@code run(...)} output, for inputs that cross window boundaries at
 * arbitrary offsets and for both the test chunker (512/64) and the default 64 KiB chunker.
 */
class StreamCheckParityTest {

    private static final String EMAIL = "visal@example.com";

    private static Map<String, List<String>> streamed(final GuardrailCheck check,
                                                      final String input,
                                                      final int windowSize,
                                                      final int overlap) {
        final var sink = new MatchAccumulator();
        check.toStream().scan(new StringReader(input), sink);
        return sink.toMaskEntities();
    }

    private static void assertParity(final GuardrailCheck check,
                                     final String input) {
        assertParity(check, input, 512, 64);
        assertParity(check, input, 65536, 1024);
    }

    private static void assertParity(final GuardrailCheck check,
                                     final String input,
                                     final int windowSize,
                                     final int overlap) {
        assertParity(check, input, windowSize, overlap, false);
    }

    private static void assertParity(final GuardrailCheck check,
                                     final String input,
                                     final int windowSize,
                                     final int overlap,
                                     final boolean orderedKeys) {
        final var memory = check.run(input);
        final var streamed = streamed(check, input, windowSize, overlap);
        final var memoryKeys = memory.maskEntities().keySet();
        if (orderedKeys) {
            assertThat(streamed.keySet()).containsExactlyElementsOf(memoryKeys);
        } else {
            assertThat(streamed.keySet()).containsExactlyInAnyOrderElementsOf(memoryKeys);
        }
        for (final var entry : memory.maskEntities().entrySet()) {
            assertThat(streamed.get(entry.getKey()))
                    .containsExactlyElementsOf(entry.getValue());
        }
        assertThat(streamed.isEmpty()).isEqualTo(!memory.detected());
    }

    @Test
    @DisplayName("secret scan equals in-memory run over repeated high-entropy tokens")
    void secretParity() {
        final var check = new SecretKeysCheck(SecretConfig.DEFAULTS, new Redactor());
        final var input = "my_AbC123xYz78qR9 then my_gpk3Kd0QxZ9mN4 and my_AbC123xYz78qR9 again "
                + "my_3x7pQ2kL9mN4aB8 plus my_gpk3Kd0QxZ9mN4 done ";

        assertParity(check, input.repeat(5));
    }

    @Test
    @DisplayName("a secret ending exactly on the test-window boundary keeps parity")
    void secretEndingOnBoundaryParity() {
        final var token = "A1b2C3d4E5f6G7h8A2";
        final var check = new SecretKeysCheck(SecretConfig.DEFAULTS, new Redactor());
        final var input = "s".repeat(512 - token.length()) + token;

        assertParity(check, input, 512, 64);
    }

    @Test
    @DisplayName("a secret straddling the default 64 KiB boundary keeps parity")
    void secretStraddlingDefaultBoundaryParity() {
        final var token = "B2d4F6h8J0k2M4n6P8q0";
        final var check = new SecretKeysCheck(SecretConfig.DEFAULTS, new Redactor());
        final var input = "s".repeat(65501) + token + " tail";

        assertParity(check, input);
    }

    @Test
    @DisplayName("custom secret patterns keep parity across windows")
    void customSecretPatternParity() {
        final var pattern = Pattern.compile("API_KEY_[A-Z0-9_]+");
        final var config = new SecretConfig(SecretConfig.DEFAULTS.preset(),
                Map.of("api_doc", List.of(pattern), "oauth", List.of(Pattern.compile("Bearer\\s+tok_[a-z0-9]+"))));
        final var check = new SecretKeysCheck(config, new Redactor());
        final var input = "call API_KEY_ABC123 and API_KEY_XYZ789 plus Bearer tok_ab12cd34 end ";

        assertParity(check, input.repeat(4));
    }

    @Test
    @DisplayName("URL scan equals in-memory run across schemes, domains, and IPs")
    void urlParity() {
        final var check = new UrlsCheck(new UrlsConfig(List.of("https"), List.of()), new Redactor());
        final var input = "ok https://example.com/x then bad http://visor.example/path?a=1 "
                + "and visal.example.org with IP 10.2.3.4:8080 maybe http://ok.example now ";

        assertParity(check, input.repeat(4));
    }

    @Test
    @DisplayName("keyword scan keeps first-seen case-folded dedupe regardless of windows")
    void keywordParity() {
        final var check = new KeywordsCheck(
                new KeywordsConfig(true, List.of("urgent", "CONFIDENTIAL")), new Redactor());
        final var input = "Please mark URGENT and confidential matters. urgent again! "
                + "Also CONFIDENTIAL now ";

        assertParity(check, input.repeat(5));
    }

    @Test
    @DisplayName("PII aggregation keeps entity key order and token lists in both paths")
    void piiParityWithOrderedKeys() {
        final var config = new PiiConfig(
                PiiCoverage.SELECTED,
                Set.of(PiiEntity.EMAIL_ADDRESS, PiiEntity.LOCATION),
                List.of(new CustomRegexConfig("employee_id", List.of(Pattern.compile("ID-[A-Z0-9]+")))),
                false);
        final var check = new PiiCheck(config, new Redactor());
        final var input = "x".repeat(494) + " " + EMAIL + "w".repeat(500) + " " + "Main Street"
                + " with ID-ABC123 and markers" + "y".repeat(40);

        assertParity(check, input, 512, 64, true);
        assertParity(check, input, 65536, 1024, true);
    }

    @Test
    @DisplayName("a location ending exactly on a non-final window end keeps parity")
    void locationEndingOnBoundaryParity() {
        final var config = new PiiConfig(
                PiiCoverage.SELECTED,
                Set.of(PiiEntity.LOCATION),
                List.of(),
                false);
        final var check = new PiiCheck(config, new Redactor());
        final var input = "z".repeat(512) + "u".repeat(500) + " " + "Main Street" + " trailing";

        assertParity(check, input, 512, 64, true);
    }

    @Test
    @DisplayName("a keyword starting exactly on a window base keeps parity")
    void keywordAtWindowBaseParity() {
        final var check = new KeywordsCheck(
                new KeywordsConfig(true, List.of("urgent")), new Redactor());
        final var inputWordPrev = "a".repeat(448) + "urgent" + " tail";
        final var inputSpacePrev = "a".repeat(447) + " urgent" + " tail";

        assertParity(check, inputWordPrev, 512, 64);
        assertParity(check, inputSpacePrev, 512, 64);
    }

    @Test
    @DisplayName("clean text produces empty output in both paths")
    void cleanTextParity() {
        final var check = new PiiCheck(PiiConfig.DEFAULTS, new Redactor());
        final var input = "SOK and VISAL met in Phnom Penh for lunch at Siem Reap. "
                + "No tokens here. ".repeat(6);

        assertParity(check, input);
    }
}