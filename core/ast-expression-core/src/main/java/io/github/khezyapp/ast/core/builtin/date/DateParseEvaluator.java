package io.github.khezyapp.ast.core.builtin.date;

import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Evaluator parsing a date/time string into an {@link java.time.Instant}.
 * <p>
 * Requires positional argument {@code input} (string) and named argument
 * {@code pattern}. Uses {@link java.time.format.DateTimeFormatter#parseBest}
 * to handle various temporal types (ZonedDateTime, LocalDateTime, LocalDate).
 * </p>
 */
public class DateParseEvaluator implements Evaluator {
    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var input = (String) args.positional().get(0);
        final var pattern = (String) args.named().get("pattern");
        final var zone = (String) args.named().getOrDefault("zone", "UTC");

        if (Objects.isNull(pattern)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "pattern is required", "named:pattern"));
        }
        if (Objects.isNull(input)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.ARGUMENT_TYPE_MISMATCH,
                            "input string is required", "positional:0"));
        }

        try {
            final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern)
                    .withLocale(Locale.US);
            final var parsed = fmt.parseBest(input,
                    ZonedDateTime::from,
                    LocalDateTime::from,
                    LocalDate::from);
            final Instant result = parsed instanceof Instant i
                    ? i
                    : parsed instanceof ZonedDateTime z
                    ? z.toInstant()
                    : parsed instanceof LocalDateTime ldt
                    ? ldt.atZone(ZoneId.of(zone)).toInstant()
                    : ((LocalDate) parsed).atStartOfDay(ZoneId.of(zone)).toInstant();
            return EvaluationOutcome.success(result);
        } catch (final Exception e) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.RUNTIME_ERROR,
                            "Date parse error: " + e.getMessage()));
        }
    }
}
