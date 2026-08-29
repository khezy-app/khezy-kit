package io.github.khezyapp.dpriv.api;

import java.util.List;

/**
 * A single audit entry (design §5.3). Carries metadata only — never raw token values.
 *
 * @param entityType  the entity type
 * @param validations the check results that produced this record
 * @param rawText     the raw text the record relates to
 */
public record AuditRecord(
        String entityType,
        List<GuardrailResult> validations,
        String rawText) {
}
