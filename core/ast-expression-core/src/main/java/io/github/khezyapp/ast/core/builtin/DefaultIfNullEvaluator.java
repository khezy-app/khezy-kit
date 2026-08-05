package io.github.khezyapp.ast.core.builtin;

import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

import java.util.Objects;

/**
 * Evaluator for defaultIfNull: returns the value if non-null, otherwise the default.
 * <p>
 * Accepts two positional arguments: the value to check and an optional default.
 * If the default is not provided and the value is null, returns {@code null}.
 * </p>
 */
public class DefaultIfNullEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var value = args.positional().get(0);
        final var defaultValue = args.positional().size() > 1
                ? args.positional().get(1) : null;
        if (Objects.nonNull(value)) {
            return EvaluationOutcome.success(value);
        }
        return EvaluationOutcome.success(defaultValue);
    }
}
