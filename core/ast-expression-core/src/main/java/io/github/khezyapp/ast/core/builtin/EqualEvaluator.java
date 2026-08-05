package io.github.khezyapp.ast.core.builtin;

import java.util.Objects;

import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

/**
 * Evaluator for equality comparison (eq).
 * <p>
 * Uses {@link Objects#equals} for non-null values. Returns {@code true} only
 * when both values are null (identity check), or false when one is null.
 * </p>
 */
public class EqualEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var left = args.positional().get(0);
        final var right = args.positional().get(1);
        if (Objects.isNull(left) || Objects.isNull(right)) {
            return EvaluationOutcome.success(left == right);
        }
        return EvaluationOutcome.success(Objects.equals(left, right));
    }
}
