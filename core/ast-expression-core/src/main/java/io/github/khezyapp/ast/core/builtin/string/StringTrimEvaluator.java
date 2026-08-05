package io.github.khezyapp.ast.core.builtin.string;

import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

import java.util.Objects;

/**
 * Evaluator trimming whitespace from both ends of a string.
 * <p>
 * Returns an empty string for null input.
 * </p>
 */
public class StringTrimEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var input = (String) args.positional().get(0);
        if (Objects.isNull(input)) {
            return EvaluationOutcome.success("");
        }
        return EvaluationOutcome.success(input.trim());
    }
}
