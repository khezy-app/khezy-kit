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
 * Evaluator for string search-and-replace.
 * <p>
 * Requires the named arguments {@code target} and {@code replacement}. When
 * the named argument {@code regex} is {@code true}, uses regex-based replacement
 * ({@link String#replaceAll}); otherwise uses literal replacement
 * ({@link String#replace}).
 * </p>
 */
public class StringReplaceEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var input = (String) args.positional().get(0);
        final var target = (String) args.named().get("target");
        final var replacement = (String) args.named().get("replacement");
        final boolean regex = (boolean) args.named().getOrDefault("regex", false);

        if (Objects.isNull(input)) {
            return EvaluationOutcome.success("",
                    Map.of("target", target, "replacement", replacement, "regex", regex));
        }
        if (Objects.isNull(target)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "Required named argument 'target' is missing",
                            "named:target"));
        }
        if (Objects.isNull(replacement)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "Required named argument 'replacement' is missing",
                            "named:replacement"));
        }

        try {
            final String result;
            if (regex) {
                result = input.replaceAll(target, replacement);
            } else {
                result = input.replace(target, replacement);
            }
            return EvaluationOutcome.success(result,
                    Map.of("input", input, "target", target, "replacement", replacement, "regex", regex));
        } catch (final Exception e) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.INVALID_REGEX,
                            "Regex replace error: " + e.getMessage()));
        }
    }
}
