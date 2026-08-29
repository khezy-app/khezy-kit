package io.github.khezyapp.dpriv.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 11: the streaming facade ({@link Guardrails#scan(Reader)} /
 * {@link Guardrails#redact(Reader, Writer)}) is parity-equal to the in-memory paths, never invokes
 * the LLM stage, and is deterministic across repeated concurrent runs.
 */
class GuardrailsStreamingTest {

    private static final String FIXTURE =
            "Email visal@example.com token wJalrXUtnFEMIK7p2x1qK visit http://example.com/page confidential ";

    private static GuardrailsConfig config() {
        return GuardrailsConfig.builder()
                .urls(new UrlsConfig(List.of(), List.of()))
                .keywords(new KeywordsConfig(true, List.of("confidential")))
                .build();
    }

    private static Reader oneShot(final String text) {
        final var bytes = text.getBytes(StandardCharsets.UTF_8);
        return new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
    }

    private static String bigMultiWindow(final String base) {
        return base.repeat(800);
    }

    @Test
    @DisplayName("streaming scan reports the same mask entities as the in-memory scan (multi-window)")
    void scanParityMultiWindow() {
        final var guardrails = Guardrails.builder().config(config()).build();
        final var text = bigMultiWindow(FIXTURE);

        final var fromString = guardrails.scan(text);
        final var fromReader = guardrails.scan(new StringReader(text));

        assertThat(fromReader.entityTypes()).containsExactlyInAnyOrderElementsOf(fromString.entityTypes());
        assertThat(fromReader.maskEntities()).isEqualTo(fromString.maskEntities());
        assertThat(fromReader.detected()).isEqualTo(fromString.detected());
    }

    @Test
    @DisplayName("streaming redact equals the in-memory redact (multi-window)")
    void redactParityMultiWindow() {
        final var guardrails = Guardrails.builder().config(config()).build();
        final var text = bigMultiWindow(FIXTURE);

        final var expected = guardrails.redact(text);
        final var out = new StringWriter();
        guardrails.redact(new StringReader(text), out);

        assertThat(out.toString()).isEqualTo(expected);
    }

    @Test
    @DisplayName("a token straddling the 64 KiB window boundary keeps streaming==in-memory parity")
    void boundaryStraddleParity() {
        final var guardrails = Guardrails.builder().config(GuardrailsConfig.DEFAULTS).build();
        final var token = "visal@example.com";
        final var text = "a".repeat(65520) + " " + token + " tail";

        final var fromString = guardrails.scan(text);
        final var fromReader = guardrails.scan(new StringReader(text));

        assertThat(fromReader.entityTypes()).containsExactlyInAnyOrderElementsOf(fromString.entityTypes());
        assertThat(fromReader.maskEntities()).isEqualTo(fromString.maskEntities());
        assertThat(fromReader.maskEntities()).containsKey("pii_email_address");
    }

    @Test
    @DisplayName("a one-shot, non-resettable reader is redacted correctly via the buffering path")
    void oneShotReaderRedaction() {
        final var guardrails = Guardrails.builder().config(config()).build();
        final var out = new StringWriter();
        guardrails.redact(oneShot(FIXTURE), out);

        final var expected = guardrails.redact(FIXTURE);
        assertThat(out.toString()).isEqualTo(expected);
        assertThat(out.toString())
                .contains("<EMAIL_ADDRESS>", "<SECRET>", "<LINK>", "<KEYWORD>")
                .doesNotContain("visal@example.com", "wJalrXUtnFEMIK7p2x1qK",
                        "http://example.com/page", "confidential");
    }

    @Test
    @DisplayName("empty and tiny inputs yield an empty, error-free scan outcome")
    void emptyAndTinyInputs() {
        final var guardrails = Guardrails.builder().config(config()).build();

        final var empty = guardrails.scan(new StringReader(""));
        assertThat(empty.detected()).isFalse();
        assertThat(empty.maskEntities()).isEmpty();
        assertThat(empty.errorMessages()).isEmpty();
        assertThat(empty.entityTypes()).isEmpty();

        final var tiny = guardrails.scan(new StringReader("a"));
        assertThat(tiny.detected()).isFalse();
        assertThat(tiny.maskEntities()).isEmpty();
        assertThat(tiny.errorMessages()).isEmpty();
    }

    @Test
    @DisplayName("streaming scan and redact never invoke the LLM classifier")
    void streamingNeverInvokesLlm() {
        final var capturing = new CapturingClassifier("jailbreak", true, 0.9);
        final var guardrails = Guardrails.builder()
                .config(config())
                .withClassifier(capturing)
                .build();

        final var out = new StringWriter();
        guardrails.scan(new StringReader(FIXTURE));
        guardrails.redact(new StringReader(FIXTURE), out);

        assertThat(capturing.saw()).isNull();
        assertThat(out.toString()).contains("<EMAIL_ADDRESS>", "<SECRET>", "<LINK>", "<KEYWORD>");
    }

    @Test
    @DisplayName("streaming scan is deterministic and identical across 50 repeated runs")
    void streamingDeterministicAcrossRuns() {
        final var guardrails = Guardrails.builder().config(config()).build();
        final var text = bigMultiWindow(FIXTURE);

        final var first = guardrails.scan(new StringReader(text));
        for (var i = 0; i < 50; i++) {
            final var again = guardrails.scan(new StringReader(text));
            assertThat(again).isEqualTo(first);
            assertThat(again.maskEntities()).isEqualTo(first.maskEntities());
        }
    }

    private static class CapturingClassifier implements LlmClassifier {

        private final String bean;
        private final boolean flagged;
        private final double confidence;
        private String seen;

        private CapturingClassifier(final String bean,
                                    final boolean flagged,
                                    final double confidence) {
            this.bean = bean;
            this.flagged = flagged;
            this.confidence = confidence;
        }

        @Override
        public Verdict classify(final String input) {
            this.seen = input;
            return new Verdict(flagged, confidence);
        }

        @Override
        public String beanName() {
            return bean;
        }

        private String saw() {
            return seen;
        }
    }
}
