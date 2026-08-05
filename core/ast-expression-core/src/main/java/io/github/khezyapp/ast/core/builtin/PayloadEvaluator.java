package io.github.khezyapp.ast.core.builtin;

import java.util.Map;
import java.util.Objects;
import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

/**
 * Evaluator for accessing fields from the evaluation context's payload body.
 * <p>
 * Supports dot-separated paths for nested navigation (e.g., "address.city").
 * Returns {@link StandardErrors#MISSING_FIELD} if a path segment does not exist,
 * and {@link StandardErrors#RUNTIME_ERROR} if a non-Map value is encountered
 * during navigation.
 * </p>
 */
public class PayloadEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var fieldName = (String) args.named().get("fieldName");
        if (Objects.isNull(fieldName)) {
            return EvaluationOutcome.failure(
                EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                    "fieldName is required", "named:fieldName"));
        }

        return navigate(ctx.getBody(), fieldName);
    }

    private static EvaluationOutcome navigate(final Object root,
                                               final String path) {
        final var segments = path.split("\\.");
        Object current = root;

        for (var i = 0; i < segments.length; i++) {
            final var key = segments[i];
            if (!(current instanceof Map<?, ?> map)) {
                return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.RUNTIME_ERROR,
                        "Cannot navigate into '" + key
                        + "': value is not a Map"));
            }
            if (!map.containsKey(key)) {
                return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_FIELD,
                        "Field '" + key + "' not found in payload",
                        "named:fieldName"));
            }
            current = map.get(key);
            if (Objects.isNull(current) && i < segments.length - 1) {
                return EvaluationOutcome.success(null);
            }
        }

        return EvaluationOutcome.success(current);
    }
}
