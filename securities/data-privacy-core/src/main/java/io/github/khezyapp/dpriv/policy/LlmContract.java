package io.github.khezyapp.dpriv.policy;

import io.github.khezyapp.dpriv.api.GuardrailResult;
import io.github.khezyapp.dpriv.api.LlmCheckConfig;
import io.github.khezyapp.dpriv.api.LlmClassifier;

import java.util.Map;
import java.util.Objects;

/**
 * Decision logic for LLM-as-judge checks (design §11.2). Pure functions over {@link LlmClassifier.Verdict}s and
 * strings — no LLM client, no I/O — so every rule is testable without any model.
 *
 * <p>The single contract rule: a verdict triggers detection iff {@code flagged} is set AND the (clamped)
 * confidence is at or above the configured threshold. A threshold {@code <= 0} disables the confidence gate, so
 * only {@code flagged} decides. LLM checks are classificatory only (design §12.2): they never redact,
 * contributing an empty {@code maskEntities} and an unchanged {@code cleanedValue}.
 */
public final class LlmContract {

    private LlmContract() {
    }

    /**
     * Applies the threshold decision rule.
     *
     * @param verdictFlagged the classifier's flagged signal
     * @param confidence     the classifier's confidence; clamped into {@code [0,1]} before comparison
     * @param threshold      the configured threshold; {@code <= 0} means only {@code verdictFlagged} gates
     * @return {@code true} iff the verdict triggers detection
     */
    public static boolean classify(final boolean verdictFlagged,
                                   final double confidence,
                                   final double threshold) {
        if (!verdictFlagged) {
            return false;
        }
        if (threshold <= 0) {
            return true;
        }
        return clamp(confidence) >= threshold;
    }

    /**
     * Maps a classifier verdict to the core {@link GuardrailResult} contract.
     *
     * <p>The resulting {@code entityType} is the classifier's {@link LlmClassifier#beanName()} (e.g.
     * {@code "jailbreak"}); {@code maskEntities} is always empty and {@code cleanedValue} always equals
     * {@code input} — LLM checks are classificatory, never redacting (design §12.2).
     *
     * <p>A disabled config short-circuits to a pass before consulting the verdict. {@code LlmCheck} normally
     * short-circuits earlier to avoid a model call, but this guard keeps {@code toResult} safe to call directly.
     *
     * @param classifier the classifier that produced the verdict; its bean name becomes the {@code entityType}
     * @param verdict    the verdict to convert
     * @param config     the check's configuration
     * @param input      the original input, kept as the {@code cleanedValue}
     * @return the immutable {@link GuardrailResult}
     */
    public static GuardrailResult toResult(final LlmClassifier classifier,
                                           final LlmClassifier.Verdict verdict,
                                           final LlmCheckConfig config,
                                           final String input) {
        Objects.requireNonNull(classifier, "classifier");
        Objects.requireNonNull(verdict, "verdict");
        Objects.requireNonNull(config, "config");
        final var entityType = classifier.beanName();
        if (!config.enabled()) {
            return GuardrailResult.pass(entityType, input);
        }
        final var detected = classify(verdict.flagged(), verdict.confidence(), config.threshold());
        if (detected) {
            return GuardrailResult.fail(entityType, input, Map.of());
        }
        return GuardrailResult.pass(entityType, input);
    }

    private static double clamp(final double confidence) {
        return Math.min(1.0d, Math.max(0.0d, confidence));
    }
}
