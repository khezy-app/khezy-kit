package io.github.khezyapp.ast.core.builtin.date;

import io.github.khezyapp.ast.core.CoreUtils;
import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Objects;

/**
 * Evaluator adding a duration to a date/time value.
 * <p>
 * Requires positional argument {@code date} and named argument {@code amount}.
 * Optional named arguments: {@code unit} (default: "seconds") and {@code zone}
 * (default: "UTC").
 * </p>
 */
public class DatePlusEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var dateObj = args.positional().get(0);
        final var amountObj = args.named().get("amount");
        final var unitName = (String) args.named().getOrDefault("unit", "seconds");
        final var zone = (String) args.named().getOrDefault("zone", "UTC");

        if (Objects.isNull(amountObj)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "amount is required", "named:amount"));
        }
        if (Objects.isNull(dateObj)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.ARGUMENT_TYPE_MISMATCH,
                            "date is required", "positional:0"));
        }

        final long amount = ((Number) amountObj).longValue();
        final ChronoUnit unit = CoreUtils.parseUnit(unitName);
        if (Objects.isNull(unit)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.RUNTIME_ERROR,
                            "Invalid unit: " + unitName));
        }

        try {
            final Temporal result = CoreUtils.addToTemporal(CoreUtils.toTemporal(dateObj, zone), amount, unit);
            return EvaluationOutcome.success(result);
        } catch (final Exception e) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.RUNTIME_ERROR,
                            "Date arithmetic error: " + e.getMessage()));
        }
    }

}
