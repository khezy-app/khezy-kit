package io.github.khezyapp.ast.core.builtin;

import java.util.Objects;
import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.model.FunctionId;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

/**
 * Evaluator for comparison operations: gt, gte, lt, lte.
 * <p>
 * Supports comparison of {@link Number} instances via {@link Double#compare}
 * and {@link Comparable} instances for other types. Incomparable types
 * produce an {@link StandardErrors#ARGUMENT_TYPE_MISMATCH} error.
 * </p>
 */
public class ComparisonEvaluator implements Evaluator {
    private final FunctionId functionId;

    /**
     * Creates an evaluator for the given comparison function.
     *
     * @param functionId the comparison function identifier (gt, gte, lt, lte)
     */
    public ComparisonEvaluator(final FunctionId functionId) {
        this.functionId = functionId;
    }

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var left = args.positional().get(0);
        final var right = args.positional().get(1);

        if (Objects.isNull(left) || Objects.isNull(right)) {
            return EvaluationOutcome.success(null);
        }

        final int cmp;
        try {
            cmp = compare(left, right);
        } catch (final IllegalArgumentException e) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.ARGUMENT_TYPE_MISMATCH, e.getMessage()));
        }

        final var fn = functionId.value();
        return switch (fn) {
            case "gt"  -> EvaluationOutcome.success(cmp > 0);
            case "gte" -> EvaluationOutcome.success(cmp >= 0);
            case "lt"  -> EvaluationOutcome.success(cmp < 0);
            case "lte" -> EvaluationOutcome.success(cmp <= 0);
            default    -> EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.RUNTIME_ERROR,
                            "Unsupported comparison: " + fn));
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compare(final Object a,
                               final Object b) {
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }
        if (a instanceof Comparable ca && b instanceof Comparable cb) {
            return ca.compareTo(cb);
        }
        throw new IllegalArgumentException(
                "Cannot compare values of types " + a.getClass().getName()
                + " and " + b.getClass().getName());
    }
}
