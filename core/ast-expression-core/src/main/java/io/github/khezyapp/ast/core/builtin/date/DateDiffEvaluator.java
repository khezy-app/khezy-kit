package io.github.khezyapp.ast.core.builtin.date;

import io.github.khezyapp.ast.core.CoreUtils;
import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

import java.util.Objects;

/**
 * Evaluator computing the difference between two date/time values.
 * <p>
 * Requires positional argument {@code start} and named argument {@code end}.
 * Returns the difference in the specified {@code unit} (default: "seconds").
 * </p>
 */
public class DateDiffEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var startObj = args.positional().get(0);
        final var endObj = args.named().get("end");
        final var unitName = (String) args.named().getOrDefault("unit", "seconds");
        final var zone = (String) args.named().getOrDefault("zone", "UTC");

        if (Objects.isNull(endObj)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "end is required", "named:end"));
        }
        if (Objects.isNull(startObj)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.ARGUMENT_TYPE_MISMATCH,
                            "start date is required", "positional:0"));
        }

        final var unit = CoreUtils.parseUnit(unitName);
        if (Objects.isNull(unit)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.RUNTIME_ERROR,
                            "Invalid unit: " + unitName));
        }

        try {
            final var start = CoreUtils.toInstant(startObj, zone);
            final var end = CoreUtils.toInstant(endObj, zone);
            final var diff = unit.between(start, end);
            return EvaluationOutcome.success(diff);
        } catch (final Exception e) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.RUNTIME_ERROR,
                            "Date difference error: " + e.getMessage()));
        }
    }

}
