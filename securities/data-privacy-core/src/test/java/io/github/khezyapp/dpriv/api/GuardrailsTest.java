package io.github.khezyapp.dpriv.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the in-memory {@link Guardrails} facade: scan/redact/run, the SANITIZE short-circuit
 * that never invokes LLM checks, the disabled-classifier short-circuit, the {@code failOnlyOnErrors}
 * toggle, and deterministic outcomes across repeated parallel runs.
 */
class GuardrailsTest {

    private static final String FIXTURE =
            "Email visal@example.com token wJalrXUtnFEMIK7p2x1qK visit https://example.com/page confidential";

    private static final String NEUTRAL = "this is a perfectly safe sentence about the weather";

    private static GuardrailsConfig config() {
        return GuardrailsConfig.builder()
                .urls(new UrlsConfig(List.of(), List.of()))
                .keywords(new KeywordsConfig(true, List.of("confidential")))
                .build();
    }

    @Test
    @DisplayName("scan detects the deterministic families and reports their entity types")
    void scanDetectsAndReportsEntityTypes() {
        final var guardrails = Guardrails.builder().config(config()).build();
        final var outcome = guardrails.scan(FIXTURE);

        assertThat(outcome.detected()).isTrue();
        assertThat(outcome.entityTypes())
                .containsExactlyInAnyOrder("pii_email_address", "secret", "link", "keyword");
        assertThat(outcome.maskEntities().keySet())
                .containsExactlyInAnyOrder("pii_email_address", "secret", "link", "keyword");
    }

    @Test
    @DisplayName("redact replaces all four families")
    void redactReplacesAll() {
        final var guardrails = Guardrails.builder().config(config()).build();
        final var redacted = guardrails.redact(FIXTURE);

        assertThat(redacted).contains("<EMAIL_ADDRESS>", "<SECRET>", "<LINK>", "<KEYWORD>");
        assertThat(redacted).doesNotContain("visal@example.com", "wJalrXUtnFEMIK7p2x1qK",
                "https://example.com/page", "confidential");
    }

    @Test
    @DisplayName("run(CLASSIFY) detects when a classifier flags")
    void classifyDetectsOnFlag() {
        final var guardrails = Guardrails.builder()
                .config(GuardrailsConfig.DEFAULTS)
                .withClassifier(new FlaggingClassifier("jailbreak", true, 0.9))
                .build();
        final var outcome = guardrails.run(NEUTRAL, Operation.CLASSIFY);

        assertThat(outcome.detected()).isTrue();
        assertThat(outcome.entityType()).isEqualTo("jailbreak");
    }

    @Test
    @DisplayName("a disabled classifier config short-circuits without invoking the model")
    void disabledClassifierShortCircuits() {
        final var capturing = new CapturingClassifier("jailbreak", true, 0.9);
        final var guardrails = Guardrails.builder()
                .config(GuardrailsConfig.builder()
                        .jailbreak(new LlmCheckConfig(false, 0.7))
                        .build())
                .withClassifier(capturing)
                .build();
        final var outcome = guardrails.run(NEUTRAL, Operation.CLASSIFY);

        assertThat(outcome.detected()).isFalse();
        assertThat(capturing.saw()).isNull();
    }

    @Test
    @DisplayName("run(SANITIZE) never invokes any classifier")
    void sanitizeNeverInvokesClassifier() {
        final var capturing = new CapturingClassifier("jailbreak", true, 0.9);
        final var guardrails = Guardrails.builder()
                .config(config())
                .withClassifier(capturing)
                .build();
        final var redacted = guardrails.run(FIXTURE, Operation.SANITIZE);

        assertThat(capturing.saw()).isNull();
        assertThat(redacted.text()).contains("<EMAIL_ADDRESS>", "<SECRET>", "<LINK>", "<KEYWORD>");
        assertThat(redacted.detected()).isFalse();
    }

    @Test
    @DisplayName("failOnlyOnErrors=true turns a classifier error into a detected/errored outcome")
    void classifierErrorFailsWhenFailOnlyOnErrors() {
        final var guardrails = Guardrails.builder()
                .config(GuardrailsConfig.DEFAULTS)
                .withClassifier(new ThrowingClassifier("jailbreak"))
                .failOnlyOnErrors(true)
                .build();
        final var outcome = guardrails.run(NEUTRAL, Operation.CLASSIFY);

        assertThat(outcome.detected()).isTrue();
        assertThat(outcome.messages()).isNotEmpty();
    }

    @Test
    @DisplayName("failOnlyOnErrors=false treats a classifier error as a pass with no messages")
    void classifierErrorPassedWhenNotFailOnlyOnErrors() {
        final var guardrails = Guardrails.builder()
                .config(GuardrailsConfig.DEFAULTS)
                .withClassifier(new ThrowingClassifier("jailbreak"))
                .failOnlyOnErrors(false)
                .build();
        final var outcome = guardrails.run(NEUTRAL, Operation.CLASSIFY);

        assertThat(outcome.detected()).isFalse();
        assertThat(outcome.messages()).isEmpty();
    }

    @Test
    @DisplayName("scan and classify produce identical, deterministically-ordered outcomes across 50 runs")
    void outcomesAreDeterministicAcrossParallelRuns() {
        final var guardrails = Guardrails.builder()
                .config(config())
                .withClassifier(new FlaggingClassifier("jailbreak", true, 0.9))
                .withClassifier(new FlaggingClassifier("nsfw", false, 0.9))
                .withClassifier(new FlaggingClassifier("topical", true, 0.5))
                .build();

        final var firstScan = guardrails.scan(FIXTURE);
        final var firstClassify = guardrails.run(NEUTRAL, Operation.CLASSIFY);
        final var expectedOrder = List.of("pii", "secret", "link", "keyword", "jailbreak", "nsfw", "topical");

        for (var i = 0; i < 50; i++) {
            assertThat(guardrails.scan(FIXTURE)).isEqualTo(firstScan);
            final var classify = guardrails.run(NEUTRAL, Operation.CLASSIFY);
            assertThat(classify).isEqualTo(firstClassify);
            final var order = classify.validations().stream()
                    .map(GuardrailResult::entityType)
                    .toList();
            assertThat(order).containsExactlyElementsOf(expectedOrder);
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

    private static final class FlaggingClassifier extends CapturingClassifier {

        private FlaggingClassifier(final String bean,
                                  final boolean flagged,
                                  final double confidence) {
            super(bean, flagged, confidence);
        }
    }

    private static final class ThrowingClassifier implements LlmClassifier {

        private final String bean;

        private ThrowingClassifier(final String bean) {
            this.bean = bean;
        }

        @Override
        public Verdict classify(final String input) {
            throw new IllegalStateException(bean + " model unavailable");
        }

        @Override
        public String beanName() {
            return bean;
        }
    }
}
