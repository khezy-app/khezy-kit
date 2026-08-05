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
 * Evaluator checking if a string ends with a specified suffix.
 * <p>
 * Supports optional case-insensitive matching via the named argument
 * {@code caseSensitive} (default: {@code true}).
 * </p>
 */
public class StringEndsWithEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var input = (String) args.positional().get(0);
        final var suffix = (String) args.named().get("suffix");
        final boolean caseSensitive = (boolean) args.named()
                .getOrDefault("caseSensitive", true);

        if (Objects.isNull(suffix)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "suffix is required", "named:suffix"));
        }
        if (Objects.isNull(input)) {
            return EvaluationOutcome.success(false,
                    Map.of("suffix", suffix, "caseSensitive", caseSensitive));
        }

        final var a = caseSensitive ? input : input.toLowerCase();
        final var b = caseSensitive ? suffix : suffix.toLowerCase();

        final var result = a.endsWith(b);
        return EvaluationOutcome.success(result,
                Map.of("input", input, "suffix", suffix, "caseSensitive", caseSensitive));
    }
}
