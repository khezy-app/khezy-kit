package io.github.khezyapp.dpriv.policy;

import io.github.khezyapp.dpriv.api.LlmCheckConfig;
import io.github.khezyapp.dpriv.api.LlmClassifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the LLM decision logic (design §11.2): the threshold rule, confidence clamping, the disabled-config
 * short-circuit, and the classificatory {@link io.github.khezyapp.dpriv.api.GuardrailResult} shape.
 */
class LlmContractTest {

    private static final double THRESHOLD = 0.7;

    private static LlmClassifier stubClassifier(final String beanName) {
        return new LlmClassifier() {
            @Override
            public Verdict classify(final String input) {
                throw new UnsupportedOperationException("LlmContract must not classify");
            }

            @Override
            public String beanName() {
                return beanName;
            }
        };
    }

    private static LlmClassifier.Verdict verdict(final boolean flagged,
                                                 final double confidence) {
        return new LlmClassifier.Verdict(flagged, confidence);
    }

    @Test
    @DisplayName("should detect a verdict flagged exactly at the threshold")
    void flaggedAtThresholdIsDetected() {
        assertThat(LlmContract.classify(true, THRESHOLD, THRESHOLD)).isTrue();
    }

    @Test
    @DisplayName("should not detect a verdict below the threshold")
    void belowThresholdNotDetected() {
        assertThat(LlmContract.classify(true, 0.69, THRESHOLD)).isFalse();
    }

    @Test
    @DisplayName("should never detect an unflagged verdict even at full confidence")
    void unflaggedNeverDetected() {
        assertThat(LlmContract.classify(false, 1.0, THRESHOLD)).isFalse();
    }

    @Test
    @DisplayName("should clamp confidence above one before comparing")
    void confidenceAboveOneClampsAndDetects() {
        assertThat(LlmContract.classify(true, 1.5, THRESHOLD)).isTrue();
    }

    @Test
    @DisplayName("should clamp confidence below zero before comparing")
    void confidenceBelowZeroClampsAndPasses() {
        assertThat(LlmContract.classify(true, -0.5, THRESHOLD)).isFalse();
    }

    @Test
    @DisplayName("should let only the flagged bit gate when the threshold is non-positive")
    void nonPositiveThresholdIgnoresConfidence() {
        assertThat(LlmContract.classify(true, 0.0, 0.0)).isTrue();
        assertThat(LlmContract.classify(true, 0.3, 0.0)).isTrue();
        assertThat(LlmContract.classify(false, 1.0, 0.0)).isFalse();
        assertThat(LlmContract.classify(false, 0.0, -1.0)).isFalse();
    }

    @Test
    @DisplayName("should short-circuit to a pass when the config is disabled")
    void disabledConfigShortCircuitsToPass() {
        final var result = LlmContract.toResult(
                stubClassifier("jailbreak"),
                verdict(true, 1.0),
                new LlmCheckConfig(false, THRESHOLD),
                "prompt injection here");

        assertThat(result.entityType()).isEqualTo("jailbreak");
        assertThat(result.detected()).isFalse();
        assertThat(result.isPassed()).isTrue();
        assertThat(result.maskEntities()).isEqualTo(Map.of());
        assertThat(result.cleanedValue()).isEqualTo("prompt injection here");
    }

    @Test
    @DisplayName("should map a detected verdict to a classificatory fail result")
    void detectedVerdictMapsToFail() {
        final var result = LlmContract.toResult(
                stubClassifier("nsfw"),
                verdict(true, 0.85),
                new LlmCheckConfig(true, THRESHOLD),
                "some nsfw text");

        assertThat(result.entityType()).isEqualTo("nsfw");
        assertThat(result.detected()).isTrue();
        assertThat(result.isPassed()).isFalse();
        assertThat(result.maskEntities()).isEqualTo(Map.of());
        assertThat(result.cleanedValue()).isEqualTo("some nsfw text");
    }

    @Test
    @DisplayName("should map a clean verdict to a pass keeping the input as cleanedValue")
    void cleanVerdictMapsToPass() {
        final var result = LlmContract.toResult(
                stubClassifier("topical"),
                verdict(false, 0.9),
                new LlmCheckConfig(true, THRESHOLD),
                "on topic");

        assertThat(result.entityType()).isEqualTo("topical");
        assertThat(result.detected()).isFalse();
        assertThat(result.isPassed()).isTrue();
        assertThat(result.maskEntities()).isEqualTo(Map.of());
        assertThat(result.cleanedValue()).isEqualTo("on topic");
    }
}
