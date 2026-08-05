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
 * Evaluator extracting a substring from a string.
 * <p>
 * Requires the named argument {@code start} (inclusive). The optional named
 * argument {@code end} specifies the end index (exclusive). Returns empty
 * string for null input.
 * </p>
 */
public class StringSubstringEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var input = (String) args.positional().get(0);
        final var startObj = args.named().get("start");
        final var endObj = args.named().get("end");

        if (Objects.isNull(input)) {
            return EvaluationOutcome.success("",
                    Map.of("start", startObj, "end", endObj));
        }
        if (Objects.isNull(startObj)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "Required named argument 'start' is missing",
                            "named:start"));
        }

        final int start = ((Number) startObj).intValue();
        try {
            final String result;
            if (Objects.nonNull(endObj)) {
                final int end = ((Number) endObj).intValue();
                result = input.substring(start, end);
            } else {
                result = input.substring(start);
            }
            return EvaluationOutcome.success(result,
                    Map.of("input", input, "start", start, "end", endObj));
        } catch (final Exception e) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.RUNTIME_ERROR,
                            "String substring error: " + e.getMessage()));
        }
    }
}
