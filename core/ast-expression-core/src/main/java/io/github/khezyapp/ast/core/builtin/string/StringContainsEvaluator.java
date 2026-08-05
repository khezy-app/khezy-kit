package io.github.khezyapp.ast.core.builtin.string;

import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

import java.util.Map;
import java.util.Objects;

/**
 * Evaluator checking if a string contains a specified substring.
 * <p>
 * Requires the named argument {@code substring}. Returns the match result
 * with input and substring in the outcome attributes.
 * </p>
 */
public class StringContainsEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var input = (String) args.positional().get(0);
        final var substring = (String) args.named().get("substring");
        if (Objects.isNull(input) || Objects.isNull(substring)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "substring is required", "named:substring"));
        }
        final var result = input.contains(substring);
        return EvaluationOutcome.success(result,
                Map.of("input", input, "substring", substring));
    }
}
