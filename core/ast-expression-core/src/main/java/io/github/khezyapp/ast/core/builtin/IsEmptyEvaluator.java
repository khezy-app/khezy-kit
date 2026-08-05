package io.github.khezyapp.ast.core.builtin;

import java.util.Objects;

import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

import java.util.Collection;
import java.util.Map;

/**
 * Evaluator for emptiness checks (isEmpty).
 * <p>
 * Returns {@code true} for null values, blank strings, empty collections,
 * and empty maps. Returns {@code false} for all other values.
 * </p>
 */
public class IsEmptyEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var val = args.positional().get(0);
        if (Objects.isNull(val)) {
            return EvaluationOutcome.success(true);
        }
        if (val instanceof String s) {
            return EvaluationOutcome.success(s.isBlank());
        }
        if (val instanceof Collection<?> c) {
            return EvaluationOutcome.success(c.isEmpty());
        }
        if (val instanceof Map<?, ?> m) {
            return EvaluationOutcome.success(m.isEmpty());
        }
        return EvaluationOutcome.success(false);
    }
}
