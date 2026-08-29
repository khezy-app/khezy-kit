package io.github.khezyapp.dpriv.springai;

/**
 * Observability payload written by GuardrailAdvisor on the pass path only (design §7, §9).
 * A violation is carried by PolicyViolationException instead. No confidence — see INDEX R1.
 */
public record GuardrailReport(boolean passed, String entityType) { }
