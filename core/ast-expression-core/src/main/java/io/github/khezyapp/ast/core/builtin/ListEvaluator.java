package io.github.khezyapp.ast.core.builtin;

import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

import java.util.List;

/**
 * Evaluator for constructing a list from positional arguments.
 * <p>
 * Returns an unmodifiable list containing all positional argument values
 * in order. Named arguments are ignored.
 * </p>
 */
public class ListEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        return EvaluationOutcome.success(List.copyOf(args.positional()));
    }
}
