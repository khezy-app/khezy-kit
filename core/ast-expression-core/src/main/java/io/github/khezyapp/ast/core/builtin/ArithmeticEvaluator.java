package io.github.khezyapp.ast.core.builtin;

import java.util.Objects;

import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.model.FunctionId;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

/**
 * Evaluator for basic arithmetic operations: add, subtract, multiply, divide.
 * <p>
 * Operands are converted to doubles via {@link #toDouble}. Division by zero
 * returns a {@link StandardErrors#DIVISION_BY_ZERO} error.
 * </p>
 */
public class ArithmeticEvaluator implements Evaluator {
    private final FunctionId functionId;

    /**
     * Creates an evaluator for the given arithmetic function.
     *
     * @param functionId the arithmetic function identifier (add, subtract, multiply, divide)
     */
    public ArithmeticEvaluator(final FunctionId functionId) {
        this.functionId = functionId;
    }

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var left = args.positional().get(0);
        final var right = args.positional().get(1);

        if (Objects.isNull(left) || Objects.isNull(right)) {
            return EvaluationOutcome.success(null);
        }

        try {
            final var l = toDouble(left);
            final var r = toDouble(right);
            final var fn = functionId.value();

            if ("divide".equals(fn) && r == 0.0) {
                return EvaluationOutcome.failure(
                        EvaluationError.of(StandardErrors.DIVISION_BY_ZERO,
                                "Division by zero"));
            }

            final double result = switch (fn) {
                case "add" -> l + r;
                case "subtract" -> l - r;
                case "multiply" -> l * r;
                case "divide" -> l / r;
                default -> throw new IllegalStateException(
                        "Unsupported arithmetic: " + fn);
            };

            return EvaluationOutcome.success(result);
        } catch (final Exception e) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.RUNTIME_ERROR, e.getMessage()));
        }
    }

    private static double toDouble(final Object v) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(v.toString());
    }
}
