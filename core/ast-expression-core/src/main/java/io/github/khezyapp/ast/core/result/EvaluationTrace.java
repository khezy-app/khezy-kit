package io.github.khezyapp.ast.core.result;

/**
 * Trace metadata for a single evaluation result.
 * <p>
 * Records whether the result was skipped, served from cache, and the
 * duration of evaluation in nanoseconds.
 * </p>
 *
 * @param skipped      whether the node was skipped (short-circuit)
 * @param cached       whether the result was served from cache
 * @param durationNanos the evaluation duration in nanoseconds
 */
public record EvaluationTrace(boolean skipped, boolean cached, long durationNanos) {

    /** Sentinel for a freshly evaluated (non-skipped, non-cached) node. */
    public static final EvaluationTrace EVALUATED = new EvaluationTrace(false, false, 0);

    /** Sentinel for a skipped node (short-circuit). */
    public static final EvaluationTrace SKIPPED = new EvaluationTrace(true, false, 0);
}
