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
 * Evaluator for regex pattern matching.
 * <p>
 * Requires the named argument {@code regex}. Supports case-insensitive matching
 * via the named argument {@code caseSensitive} (default: {@code true}).
 * Invalid regex patterns return {@link StandardErrors#INVALID_REGEX}.
 * </p>
 */
public class StringMatchEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var input = (String) args.positional().get(0);
        final var regex = (String) args.named().get("regex");
        final boolean caseSensitive = (boolean) args.named()
                .getOrDefault("caseSensitive", true);

        if (Objects.isNull(regex)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "regex is required", "named:regex"));
        }
        if (Objects.isNull(input)) {
            return EvaluationOutcome.success(false,
                    Map.of("regex", regex, "caseSensitive", caseSensitive));
        }

        try {
            final boolean result;
            if (caseSensitive) {
                result = input.matches(regex);
            } else {
                result = input.toLowerCase().matches("(?i)" + regex);
            }
            return EvaluationOutcome.success(result,
                    Map.of("input", input, "regex", regex, "caseSensitive", caseSensitive));
        } catch (final Exception e) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.INVALID_REGEX,
                            "Invalid regex pattern: " + e.getMessage()));
        }
    }
}
