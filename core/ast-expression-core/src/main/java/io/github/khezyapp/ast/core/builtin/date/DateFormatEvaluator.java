package io.github.khezyapp.ast.core.builtin.date;

import io.github.khezyapp.ast.core.CoreUtils;
import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Evaluator formatting a date/time value as a string using a pattern.
 * <p>
 * Requires positional argument {@code date} and named argument {@code pattern}
 * (e.g., "yyyy-MM-dd HH:mm:ss"). Optional named argument {@code zone}
 * (default: "UTC").
 * </p>
 */
public class DateFormatEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var dateObj = args.positional().get(0);
        final var pattern = (String) args.named().get("pattern");
        final var zone = (String) args.named().getOrDefault("zone", "UTC");

        if (Objects.isNull(pattern)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "pattern is required", "named:pattern"));
        }
        if (Objects.isNull(dateObj)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.ARGUMENT_TYPE_MISMATCH,
                            "date is required", "positional:0"));
        }

        try {
            final var instant = CoreUtils.toInstant(dateObj, zone);
            final var fmt = DateTimeFormatter.ofPattern(pattern);
            final var zdt = instant.atZone(ZoneId.of(zone));
            return EvaluationOutcome.success(fmt.format(zdt));
        } catch (final Exception e) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.RUNTIME_ERROR,
                            "Date format error: " + e.getMessage()));
        }
    }

}
