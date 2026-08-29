package io.github.khezyapp.dpriv.api;

import java.util.List;
import java.util.Map;

/**
 * Outcome of {@link Guardrails#run(String, Operation)} (design §5.3).
 *
 * @param text          the raw (classify) or redacted (sanitize) text
 * @param entityType    the primary entity type, if any
 * @param detected      whether sensitive content was detected
 * @param validations   per-check results
 * @param maskEntities  entityType -> matched token list
 * @param auditRecords  opt-in audit metadata (never raw tokens)
 * @param messages      human-readable messages
 */
public record GuardrailsOutcome(
        String text,
        String entityType,
        boolean detected,
        List<GuardrailResult> validations,
        Map<String, List<String>> maskEntities,
        List<AuditRecord> auditRecords,
        List<String> messages) {

    /**
     * Convenience shorthand for {@code !detected()}.
     *
     * @return true if nothing was detected
     */
    public boolean isPassed() {
        return !detected;
    }
}
