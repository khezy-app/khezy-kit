package io.github.khezyapp.dynamicform.engine;

import io.github.khezyapp.dynamicform.model.Constraints;
import io.github.khezyapp.dynamicform.model.ValueType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Maps raw input to typed values per {@link ValueType} (P7).
 * <p>
 * String input is converted to the declared type (e.g. {@code "25"} → {@code Long}, {@code "true"}
 * → {@code Boolean}, ISO text → date-time). {@code DECIMAL} honors {@code scale} by rounding to the
 * declared places and flags {@code DECIMAL(p,s)} integer overflow. {@code FILE} values pass through
 * untouched — uploads are handled by the engine where the storage provider lives.
 */
public final class Coercer {

    private Coercer() {
    }

    /**
     * Coerces a raw value to the declared type.
     *
     * @param type        the target value type
     * @param raw         the raw value
     * @param constraints the constraints (used for {@code DECIMAL} scale/precision)
     * @return the coercion outcome
     */
    public static CoercionResult coerce(final ValueType type,
                                        final Object raw,
                                        final Constraints constraints) {
        if (Objects.isNull(raw)) {
            return CoercionResult.success(null);
        }
        return switch (type) {
            case STRING -> coerceString(raw);
            case NUMBER -> coerceNumber(raw);
            case DECIMAL -> coerceDecimal(raw, constraints);
            case BOOLEAN -> coerceBoolean(raw);
            case DATE_TIME -> coerceDateTime(raw);
            case ARRAY -> coerceArray(raw);
            case OBJECT -> coerceObject(raw);
            case ENUM -> coerceEnum(raw);
            case FILE -> CoercionResult.success(raw);
            default -> CoercionResult.success(raw);
        };
    }

    private static CoercionResult coerceString(final Object raw) {
        if (raw instanceof Map || raw instanceof List || raw instanceof byte[]) {
            return CoercionResult.failure("expected a string, got " + raw.getClass().getSimpleName());
        }
        return CoercionResult.success(String.valueOf(raw));
    }

    private static CoercionResult coerceNumber(final Object raw) {
        if (raw instanceof Number) {
            return CoercionResult.success(raw);
        }
        if (raw instanceof String text) {
            final var trimmed = text.strip();
            try {
                if (trimmed.matches("-?\\d+")) {
                    return CoercionResult.success(Long.parseLong(trimmed));
                }
                return CoercionResult.success(Double.parseDouble(trimmed));
            } catch (final NumberFormatException e) {
                return CoercionResult.failure("'" + text + "' is not a number");
            }
        }
        return CoercionResult.failure("expected a number, got " + raw.getClass().getSimpleName());
    }

    private static CoercionResult coerceDecimal(final Object raw,
                                                final Constraints constraints) {
        final BigDecimal parsed;
        if (raw instanceof Number number) {
            parsed = toBigDecimal(number);
        } else if (raw instanceof String text) {
            try {
                parsed = new BigDecimal(text.trim());
            } catch (final NumberFormatException e) {
                return CoercionResult.failure("'" + text + "' is not a decimal number");
            }
        } else {
            return CoercionResult.failure("expected a decimal number, got " + raw.getClass().getSimpleName());
        }

        final var scale = Objects.nonNull(constraints) ? constraints.scale() : null;
        var result = parsed;
        if (Objects.nonNull(scale)) {
            result = result.setScale(scale, RoundingMode.HALF_UP);
        }
        if (Objects.nonNull(scale) && Objects.nonNull(constraints.precision())) {
            final var integerDigits = result.precision() - result.scale();
            if (integerDigits > constraints.precision() - scale) {
                return CoercionResult.failure("value exceeds DECIMAL(" + constraints.precision() + ","
                        + scale + ")");
            }
        }
        return CoercionResult.success(result);
    }

    private static BigDecimal toBigDecimal(final Number number) {
        if (number instanceof BigDecimal decimal) {
            return decimal;
        }
        if (number instanceof Integer || number instanceof Long) {
            return new BigDecimal(number.toString());
        }
        return BigDecimal.valueOf(number.doubleValue());
    }

    private static CoercionResult coerceBoolean(final Object raw) {
        if (raw instanceof Boolean bool) {
            return CoercionResult.success(bool);
        }
        if (raw instanceof String text) {
            return switch (text.trim().toLowerCase()) {
                case "true", "1" -> CoercionResult.success(Boolean.TRUE);
                case "false", "0" -> CoercionResult.success(Boolean.FALSE);
                default -> CoercionResult.failure("'" + text + "' is not a boolean");
            };
        }
        return CoercionResult.failure("expected a boolean, got " + raw.getClass().getSimpleName());
    }

    private static CoercionResult coerceDateTime(final Object raw) {
        if (raw instanceof LocalDateTime || raw instanceof LocalDate || raw instanceof Instant
                || raw instanceof OffsetDateTime || raw instanceof ZonedDateTime) {
            return CoercionResult.success(raw);
        }
        if (raw instanceof Number number) {
            return CoercionResult.success(Instant.ofEpochMilli(number.longValue()));
        }
        if (raw instanceof String text) {
            final var trimmed = text.trim();
            final var formats = List.of(
                    LocalDateTime::parse,
                    OffsetDateTime::parse,
                    (Function<String, Object>) LocalDate::parse
            );
            for (final var format : formats) {
                try {
                    return CoercionResult.success(format.apply(trimmed));
                } catch (final DateTimeParseException e) {
                    // try the next format
                }
            }
            return CoercionResult.failure("'" + text + "' is not a valid date-time");
        }
        return CoercionResult.failure("expected a date-time, got " + raw.getClass().getSimpleName());
    }

    private static CoercionResult coerceArray(final Object raw) {
        if (raw instanceof List) {
            return CoercionResult.success(raw);
        }
        if (raw instanceof String text) {
            final var parts = text.split(",", -1);
            final var list = new ArrayList<String>();
            for (final var part : parts) {
                list.add(part.trim());
            }
            return CoercionResult.success(list);
        }
        return CoercionResult.failure("expected an array, got " + raw.getClass().getSimpleName());
    }

    private static CoercionResult coerceObject(final Object raw) {
        if (raw instanceof Map) {
            return CoercionResult.success(raw);
        }
        return CoercionResult.failure("expected an object, got " + raw.getClass().getSimpleName());
    }

    private static CoercionResult coerceEnum(final Object raw) {
        if (raw instanceof Map || raw instanceof List) {
            return CoercionResult.failure("expected a single value, got " + raw.getClass().getSimpleName());
        }
        return CoercionResult.success(String.valueOf(raw));
    }
}
