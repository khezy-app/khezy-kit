package io.github.khezyapp.ast.core.builtin;

import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

/**
 * Evaluator for logical negation (not).
 * <p>
 * Accepts a single boolean positional argument and returns its negation.
 * Non-boolean input produces an {@link StandardErrors#ARGUMENT_TYPE_MISMATCH} error.
 * </p>
 */
public class NotEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var val = args.positional().get(0);
        if (val instanceof Boolean b) {
            return EvaluationOutcome.success(!b);
        }
        return EvaluationOutcome.failure(
                EvaluationError.of(StandardErrors.ARGUMENT_TYPE_MISMATCH,
                        "NOT requires a boolean argument"));
    }
}
