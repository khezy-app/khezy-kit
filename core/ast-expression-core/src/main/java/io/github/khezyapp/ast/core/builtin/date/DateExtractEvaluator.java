package io.github.khezyapp.ast.core.builtin.date;

import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.function.Function;

/**
 * Evaluator extracting a date/time component (year, month, day, hour, minute, second).
 * <p>
 * Uses a {@link java.util.function.Function} from {@link java.time.ZonedDateTime}
 * to {@link Integer} for the specific extraction logic. Static factory methods
 * ({@link #year()}, {@link #month()}, etc.) provide pre-configured instances.
 * </p>
 */
public class DateExtractEvaluator implements Evaluator {

    private final Function<ZonedDateTime, Integer> extractor;

    public DateExtractEvaluator(final Function<ZonedDateTime, Integer> extractor) {
        this.extractor = extractor;
    }

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var dateObj = args.positional().get(0);
        final var zone = (String) args.named().getOrDefault("zone", "UTC");

        if (Objects.isNull(dateObj)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.ARGUMENT_TYPE_MISMATCH,
                            "date is required", "positional:0"));
        }

        try {
            final Instant instant = toInstant(dateObj);
            final ZonedDateTime zdt = instant.atZone(ZoneId.of(zone));
            return EvaluationOutcome.success(extractor.apply(zdt));
        } catch (final Exception e) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.RUNTIME_ERROR,
                            "Date extract error: " + e.getMessage()));
        }
    }

    private static Instant toInstant(final Object obj) {
        if (obj instanceof Instant i) {
            return i;
        }
        if (obj instanceof Number n) {
            return Instant.ofEpochMilli(n.longValue());
        }
        return Instant.parse(obj.toString());
    }

    public static DateExtractEvaluator year() {
        return new DateExtractEvaluator(ZonedDateTime::getYear);
    }

    public static DateExtractEvaluator month() {
        return new DateExtractEvaluator(ZonedDateTime::getMonthValue);
    }

    public static DateExtractEvaluator day() {
        return new DateExtractEvaluator(ZonedDateTime::getDayOfMonth);
    }

    public static DateExtractEvaluator hour() {
        return new DateExtractEvaluator(ZonedDateTime::getHour);
    }

    public static DateExtractEvaluator minute() {
        return new DateExtractEvaluator(ZonedDateTime::getMinute);
    }

    public static DateExtractEvaluator second() {
        return new DateExtractEvaluator(ZonedDateTime::getSecond);
    }
}
