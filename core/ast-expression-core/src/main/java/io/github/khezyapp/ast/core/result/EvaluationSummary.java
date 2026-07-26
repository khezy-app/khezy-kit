package io.github.khezyapp.ast.core.result;

import io.github.khezyapp.ast.core.error.EvaluationError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregate summary of an entire AST evaluation, computed from the root
 * {@link EvaluationResult}.
 * <p>
 * Provides total duration, node counts (total, skipped, cached), all errors,
 * and per-function statistics via {@link FunctionStats}.
 * Use {@link #from(EvaluationResult)} to produce a summary from any result tree.
 * </p>
 *
 * @param totalDurationNanos total evaluation time across all nodes
 * @param totalNodes         total number of evaluated nodes
 * @param skippedNodes       number of skipped (short-circuited) nodes
 * @param cachedNodes        number of cache-hit nodes
 * @param allErrors          aggregated errors from all nodes
 * @param perFunction        statistics grouped by function name
 */
public record EvaluationSummary(
        long totalDurationNanos,
        int totalNodes,
        int skippedNodes,
        int cachedNodes,
        List<EvaluationError> allErrors,
        Map<String, FunctionStats> perFunction
) {
    /**
     * Builds a summary from the root of an evaluation result tree.
     *
     * @param root the root evaluation result
     * @return a new summary with aggregated statistics
     */
    public static EvaluationSummary from(final EvaluationResult root) {
        final var all = root.flatten();
        long totalDuration = 0;
        int total = all.size();
        int skipped = 0;
        int cached = 0;
        final var errors = new ArrayList<EvaluationError>();
        final var perFunction = new HashMap<String, FunctionStats>();

        for (final var r : all) {
            totalDuration += r.trace().durationNanos();
            if (r.isSkipped()) {
                skipped++;
            }
            if (r.isCached()) {
                cached++;
            }
            errors.addAll(r.errors());
            perFunction.merge(
                    r.function().value(),
                    new FunctionStats(1,
                            r.isCached() ? 1 : 0,
                            r.isSkipped() ? 1 : 0,
                            r.trace().durationNanos()),
                    FunctionStats::add
            );
        }

        return new EvaluationSummary(
                totalDuration,
                total,
                skipped,
                cached,
                List.copyOf(errors),
                Map.copyOf(perFunction)
        );
    }
}
