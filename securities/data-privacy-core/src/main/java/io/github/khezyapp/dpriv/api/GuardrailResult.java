package io.github.khezyapp.dpriv.api;

import java.util.List;
import java.util.Map;

/**
 * Immutable result of a single check (design §5.1). Carries the verdict, the sanitized text, and
 * the per-entity matched tokens that feed redaction.
 *
 * @param entityType   the policy rule name / entityType string (e.g. {@code "pii_credit_card"},
 *                     {@code "secret"}, {@code "link"}), never null
 * @param detected     whether the input violated this check
 * @param cleanedValue the sanitized text produced by this check (used to chain masking)
 * @param maskEntities {@code entityType -> non-windowed token list}
 */
public record GuardrailResult(
        String entityType,
        boolean detected,
        String cleanedValue,
        Map<String, List<String>> maskEntities) {

    /**
     * Creates a passing result for {@code entityType} that leaves {@code cleanedValue} unchanged
     * and contributes no masks.
     */
    public static GuardrailResult pass(final String entityType,
                                       final String cleanedValue) {
        return new GuardrailResult(entityType, false, cleanedValue, Map.of());
    }

    /**
     * Creates a failing (detected) result for {@code entityType} that contributes {@code masks}
     * to redaction.
     */
    public static GuardrailResult fail(final String entityType,
                                       final String cleanedValue,
                                       final Map<String, List<String>> masks) {
        return new GuardrailResult(entityType, true, cleanedValue, masks);
    }

    /**
     * Convenience shorthand for {@code !detected()}.
     */
    public boolean isPassed() {
        return !detected;
    }
}
