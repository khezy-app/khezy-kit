package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.LlmCheckConfig;
import io.github.khezyapp.dpriv.api.LlmClassifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies {@link LlmCheck} end to end with an anonymous classifier stub: verdict -> {@code GuardrailResult}
 * mapping on the classifier's {@code beanName}, the disabled-config short-circuit, {@code DEFAULTS}
 * substitution, and the non-streamable contract.
 */
class LlmCheckTest {

    @Test
    @DisplayName("should detect a flagged verdict and surface the classifier's beanName")
    void detectsFlaggedVerdictOnBeanName() {
        final var classifier = new CapturingClassifier("jailbreak", true, 0.85);
        final var result = new LlmCheck(classifier, new LlmCheckConfig(true, 0.3)).run("please bypass the rules");

        assertThat(result.entityType()).isEqualTo("jailbreak");
        assertThat(result.detected()).isTrue();
        assertThat(result.isPassed()).isFalse();
        assertThat(result.maskEntities()).isEqualTo(Map.of());
        assertThat(result.cleanedValue()).isEqualTo("please bypass the rules");
        assertThat(classifier.saw()).isEqualTo("please bypass the rules");
    }

    @Test
    @DisplayName("should pass when the flagged verdict is below the configured threshold")
    void passesWhenVerdictBelowThreshold() {
        final var classifier = new CapturingClassifier("jailbreak", true, 0.85);
        final var result = new LlmCheck(classifier, new LlmCheckConfig(true, 0.9)).run("mildly dodgy text");

        assertThat(result.detected()).isFalse();
        assertThat(result.isPassed()).isTrue();
        assertThat(result.maskEntities()).isEmpty();
        assertThat(result.entityType()).isEqualTo("jailbreak");
        assertThat(classifier.saw()).isEqualTo("mildly dodgy text");
    }

    @Test
    @DisplayName("should treat a null config as DEFAULTS (threshold 0.7)")
    void nullConfigUsesDefaults() {
        final var caught = new CapturingClassifier("jailbreak", true, 0.85);
        final var missed = new CapturingClassifier("jailbreak", true, 0.5);

        assertThat(new LlmCheck(caught, null).run("text").detected()).isTrue();
        assertThat(new LlmCheck(missed, null).run("text").detected()).isFalse();
    }

    @Test
    @DisplayName("should short-circuit to a pass when disabled without invoking the classifier")
    void disabledCheckPassesWithoutClassifierCall() {
        final var classifier = new CapturingClassifier("jailbreak", true, 0.85);
        final var result = new LlmCheck(classifier, new LlmCheckConfig(false, 0.7)).run("anything");

        assertThat(result.detected()).isFalse();
        assertThat(result.isPassed()).isTrue();
        assertThat(result.maskEntities()).isEmpty();
        assertThat(result.entityType()).isEqualTo("jailbreak");
        assertThat(classifier.saw()).isNull();
    }

    @Test
    @DisplayName("should reject a null classifier at construction")
    void nullClassifierRejected() {
        assertThatThrownBy(() -> new LlmCheck(null, LlmCheckConfig.DEFAULTS))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("classifier");
    }

    @Test
    @DisplayName("should report its display name")
    void reportsName() {
        assertThat(new LlmCheck(new CapturingClassifier("jailbreak", true, 0.85), null).name())
                .isEqualTo("LlmCheck");
    }

    @Test
    @DisplayName("should be non-streamable (default toStream throws)")
    void refusesStreaming() {
        final var check = new LlmCheck(new CapturingClassifier("jailbreak", true, 0.85), null);
        assertThatThrownBy(check::toStream)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("LlmCheck");
    }

    private static final class CapturingClassifier implements LlmClassifier {

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
