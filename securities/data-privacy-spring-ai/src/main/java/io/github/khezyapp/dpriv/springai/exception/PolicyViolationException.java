package io.github.khezyapp.dpriv.springai.exception;

import io.github.khezyapp.dpriv.springai.ProtectionScope;

/**
 * A classifier flagged the input or output (detected, above threshold) (design §8.9, G14).
 * Never bypassable: there is no configuration that lets a detected violation through.
 */
public final class PolicyViolationException extends DataPrivacyException {

    private final String entityType;
    private final ProtectionScope scope;

    public PolicyViolationException(final String entityType,
                                    final ProtectionScope scope) {
        super("policy violation detected: " + entityType + " (scope=" + scope + ")");
        this.entityType = entityType;
        this.scope = scope;
    }

    public String entityType() {
        return entityType;
    }

    public ProtectionScope scope() {
        return scope;
    }
}
