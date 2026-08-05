package io.github.khezyapp.ast.core.builtin;

import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.model.FunctionId;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

import java.util.Objects;

/**
 * Evaluator for boolean logic operations: AND and OR.
 * <p>
 * Supports short-circuit evaluation via the engine's lazy child evaluation
 * mechanism. Both functions accept variadic boolean operands.
 * </p>
 */
public class BooleanLogicEvaluator implements Evaluator {
    private final FunctionId functionId;

    /**
     * Creates an evaluator for the given boolean logic function.
     *
     * @param functionId the boolean function identifier (and, or)
     */
    public BooleanLogicEvaluator(final FunctionId functionId) {
        this.functionId = functionId;
    }

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var fn = functionId.value();
        final boolean result = switch (fn) {
            case "and" -> args.positional().stream()
                    .filter(Objects::nonNull)
                    .allMatch(v -> toBoolean(v, true));
            case "or" -> args.positional().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(v -> toBoolean(v, false));
            default -> throw new IllegalStateException(
                    "Unsupported boolean logic: " + fn);
        };
        return EvaluationOutcome.success(result);
    }

    private static boolean toBoolean(final Object v,
                                     final boolean defaultValue) {
        if (v instanceof Boolean b) {
            return b;
        }
        return defaultValue;
    }
}
