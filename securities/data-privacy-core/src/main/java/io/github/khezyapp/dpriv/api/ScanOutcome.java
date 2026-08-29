package io.github.khezyapp.dpriv.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Outcome of {@link Guardrails#scan(String)} / {@link Guardrails#scan(Reader)} (design §5.3, §12.3).
 *
 * @param text           the scanned text
 * @param entityType     the primary entity type, if any
 * @param detected       whether sensitive content was detected
 * @param errorMessages  per-check error messages, if any
 * @param maskEntities   entityType -> matched token list
 * @param auditRecords   opt-in audit metadata (never raw tokens)
 */
public record ScanOutcome(
        String text,
        String entityType,
        boolean detected,
        List<String> errorMessages,
        Map<String, List<String>> maskEntities,
        List<AuditRecord> auditRecords) {

    /**
     * Convenience shorthand for {@code !detected()}.
     *
     * @return true if nothing was detected
     */
    public boolean isPassed() {
        return !detected;
    }

    /**
     * The distinct entity types with matches, as the key set of {@link #maskEntities()}.
     *
     * @return the entity types
     */
    public Set<String> entityTypes() {
        return maskEntities.keySet();
    }
}
