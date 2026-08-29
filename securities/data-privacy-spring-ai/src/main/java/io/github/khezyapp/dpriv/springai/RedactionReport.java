package io.github.khezyapp.dpriv.springai;

import java.util.Set;

/**
 * Observability payload written by DataPrivacyAdvisor to the request context (design §7, §9).
 */
public record RedactionReport(boolean redacted, Set<String> entityTypes) {

    public static final RedactionReport NONE = new RedactionReport(false, Set.of());
}
