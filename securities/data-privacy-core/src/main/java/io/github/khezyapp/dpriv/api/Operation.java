package io.github.khezyapp.dpriv.api;

/**
 * Operations supported by {@link Guardrails#run(String, Operation)} (design §12.2).
 */
public enum Operation {

    /**
     * Constrained LLM-as-judge: annotate + validate the input.
     */
    CLASSIFY,

    /**
     * Deterministic redaction only, no LLM involvement.
     */
    SANITIZE
}
