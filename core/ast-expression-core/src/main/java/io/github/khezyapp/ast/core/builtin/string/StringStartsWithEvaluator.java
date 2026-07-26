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
 * Evaluator checking if a string starts with a specified prefix.
 * <p>
 * Supports optional case-insensitive matching via the named argument
 * {@code caseSensitive} (default: {@code true}).
 * </p>
 */
public class StringStartsWithEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var input = (String) args.positional().get(0);
        final var prefix = (String) args.named().get("prefix");
        final boolean caseSensitive = (boolean) args.named()
                .getOrDefault("caseSensitive", true);

        if (Objects.isNull(prefix)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "prefix is required", "named:prefix"));
        }
        if (Objects.isNull(input)) {
            return EvaluationOutcome.success(false,
                    Map.of("prefix", prefix, "caseSensitive", caseSensitive));
        }

        final var a = caseSensitive ? input : input.toLowerCase();
        final var b = caseSensitive ? prefix : prefix.toLowerCase();

        final var result = a.startsWith(b);
        return EvaluationOutcome.success(result,
                Map.of("input", input, "prefix", prefix, "caseSensitive", caseSensitive));
    }
}
