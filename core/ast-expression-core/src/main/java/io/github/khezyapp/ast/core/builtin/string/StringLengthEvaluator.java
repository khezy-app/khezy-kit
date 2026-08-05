package io.github.khezyapp.ast.core.builtin.string;

import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

import java.util.Map;
import java.util.Objects;

/**
 * Evaluator returning the length of a string.
 * <p>
 * Returns 0 for null input. The result includes length and input in
 * the outcome attributes.
 * </p>
 */
public class StringLengthEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var input = (String) args.positional().get(0);
        if (Objects.isNull(input)) {
            return EvaluationOutcome.success(0,
                    Map.of("input", ""));
        }
        final var len = input.length();
        return EvaluationOutcome.success(len,
                Map.of("input", input, "length", len));
    }
}
