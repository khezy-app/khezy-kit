package io.github.khezyapp.ast.core.result;

/**
 * Per-function evaluation statistics used in {@link EvaluationSummary}.
 * <p>
 * Tracks how many times a function was evaluated, cached, skipped, and its
 * total accumulated evaluation time.
 * </p>
 *
 * @param count             number of invocations
 * @param cached            number of cache hits
 * @param skipped           number of skips (short-circuit)
 * @param totalDurationNanos total evaluation time in nanoseconds
 */
public record FunctionStats(
    int count,
    int cached,
    int skipped,
    long totalDurationNanos
) {
    /**
     * Merges another {@code FunctionStats} into this one by summing counts.
     *
     * @param other the other statistics to add
     * @return a new combined stats record
     */
    public FunctionStats add(final FunctionStats other) {
        return new FunctionStats(
            this.count + other.count,
            this.cached + other.cached,
            this.skipped + other.skipped,
            this.totalDurationNanos + other.totalDurationNanos
        );
    }
}
