package io.github.khezyapp.dpriv.api;

/**
 * LLM check configuration (design §5.4).
 *
 * @param enabled   whether this LLM check is active
 * @param threshold the confidence threshold below which a verdict is ignored (default 0.7)
 */
public record LlmCheckConfig(boolean enabled, double threshold) {

    /**
     * Defaults: enabled, threshold {@code 0.7}.
     */
    public static final LlmCheckConfig DEFAULTS = new LlmCheckConfig(true, 0.7);
}
