package io.github.khezyapp.dpriv.pipeline;

import io.github.khezyapp.dpriv.api.GuardrailsConfig;
import io.github.khezyapp.dpriv.api.KeywordsConfig;
import io.github.khezyapp.dpriv.api.LlmCheckConfig;
import io.github.khezyapp.dpriv.api.LlmClassifier;
import io.github.khezyapp.dpriv.api.UrlsConfig;
import io.github.khezyapp.dpriv.checks.LlmCheck;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the two-stage in-memory evaluator: deterministic preflight detection, merged masks,
 * combined redaction, parallel/error containment in the classificatory stage, and the
 * {@code failOnlyOnErrors} policy.
 */
class GuardrailPipelineTest {

    private static final String FIXTURE =
            "Email visal@example.com token wJalrXUtnFEMIK7p2x1qK visit https://example.com/page confidential";

    private static GuardrailsConfig config() {
        return GuardrailsConfig.builder()
                .urls(new UrlsConfig(List.of(), List.of()))
                .keywords(new KeywordsConfig(true, List.of("confidential")))
                .build();
    }

    @Test
    @DisplayName("preflight detects all four deterministic families and redacts them")
    void preflightDetectsFourFamiliesAndRedacts() {
        final var pipeline = new GuardrailPipeline(config());
        final var stage = pipeline.preflight(FIXTURE);

        assertThat(stage.detected()).isTrue();
        assertThat(stage.maskEntities().keySet())
                .containsExactlyInAnyOrder("pii_email_address", "secret", "link", "keyword");

        final var redacted = pipeline.redact(FIXTURE);
        assertThat(redacted).contains("<EMAIL_ADDRESS>", "<SECRET>", "<LINK>", "<KEYWORD>");
        assertThat(redacted).doesNotContain("visal@example.com", "wJalrXUtnFEMIK7p2x1qK",
                "https://example.com/page", "confidential");
    }

    @Test
    @DisplayName("classify stage runs LLM checks in parallel and surfaces flags")
    void classifyRunsLlmChecks() {
        final var pipeline = new GuardrailPipeline(config(),
                GuardrailPipeline.defaultPreflight(config()),
                List.of(new LlmCheck(new FlaggingClassifier("jailbreak", true, 0.9), LlmCheckConfig.DEFAULTS)),
                true);

        final var stage = pipeline.classify("neutral input with no deterministic hit");
        assertThat(stage.detected()).isTrue();
        assertThat(stage.maskEntities()).isEmpty();
        assertThat(stage.validations()).hasSize(1);
        assertThat(stage.validations().get(0).entityType()).isEqualTo("jailbreak");
    }

    @Test
    @DisplayName("SANITIZE-style redact never invokes the classificatory stage")
    void redactDoesNotInvokeClassifiers() {
        final var capturing = new CapturingClassifier("jailbreak", true, 0.9);
        final var pipeline = new GuardrailPipeline(config(),
                GuardrailPipeline.defaultPreflight(config()),
                List.of(new LlmCheck(capturing, LlmCheckConfig.DEFAULTS)),
                true);

        pipeline.redact(FIXTURE);
        assertThat(capturing.saw()).isNull();
    }

    @Test
    @DisplayName("a throwing classifier is contained and reported as an error")
    void throwingClassifierIsContained() {
        final var throwing = new ThrowingClassifier("jailbreak");
        final var flagging = new FlaggingClassifier("nsfw", true, 0.9);
        final var pipeline = new GuardrailPipeline(config(),
                GuardrailPipeline.defaultPreflight(config()),
                List.of(new LlmCheck(throwing, LlmCheckConfig.DEFAULTS),
                        new LlmCheck(flagging, LlmCheckConfig.DEFAULTS)),
                true);

        final var stage = pipeline.classify("neutral input");
        assertThat(stage.detected()).isTrue();
        assertThat(stage.errors()).isNotEmpty();
        assertThat(flagging.invoked()).isTrue();
    }

    @Test
    @DisplayName("failOnlyOnErrors=false treats a classifier error as a pass")
    void errorTreatedAsPassWhenNotFailOnlyOnErrors() {
        final var throwing = new ThrowingClassifier("jailbreak");
        final var pipeline = new GuardrailPipeline(config(),
                GuardrailPipeline.defaultPreflight(config()),
                List.of(new LlmCheck(throwing, LlmCheckConfig.DEFAULTS)),
                false);

        final var stage = pipeline.classify("neutral input");
        assertThat(stage.detected()).isFalse();
        assertThat(stage.errors()).isNotEmpty();
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

        private boolean invoked;

        private FlaggingClassifier(final String bean,
                                   final boolean flagged,
                                   final double confidence) {
            super(bean, flagged, confidence);
        }

        @Override
        public Verdict classify(final String input) {
            this.invoked = true;
            return super.classify(input);
        }

        private boolean invoked() {
            return invoked;
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
