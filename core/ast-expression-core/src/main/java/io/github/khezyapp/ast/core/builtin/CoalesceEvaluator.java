package io.github.khezyapp.ast.core.builtin;

import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

import java.util.Objects;

/**
 * Evaluator for coalesce: returns the first non-null positional argument.
 * <p>
 * Iterates through all positional arguments and returns the first one that
 * is not null. If all arguments are null, returns {@code null}.
 * </p>
 */
public class CoalesceEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var list = args.positional();
        for (final var item : list) {
            if (Objects.nonNull(item)) {
                return EvaluationOutcome.success(item);
            }
        }
        return EvaluationOutcome.success(null);
    }
}
