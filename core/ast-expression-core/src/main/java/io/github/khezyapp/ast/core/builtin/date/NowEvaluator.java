package io.github.khezyapp.ast.core.builtin.date;

import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

/**
 * Evaluator returning the current instant.
 * <p>
 * Uses the clock from the evaluation context, enabling deterministic testing
 * with a fixed clock.
 * </p>
 */
public class NowEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        return EvaluationOutcome.success(ctx.clock().instant());
    }
}
