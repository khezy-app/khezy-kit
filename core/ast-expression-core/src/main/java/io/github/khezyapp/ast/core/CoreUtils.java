package io.github.khezyapp.ast.core;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility methods used across the AST expression evaluation library.
 * <p>
 * Provides null-safe collection helpers, temporal conversion utilities,
 * and common string/collection inspection methods.
 * </p>
 */
public abstract class CoreUtils {

    /**
     * Returns an empty list if the given list is null, otherwise returns the list.
     *
     * @param <T>  the element type
     * @param list the list (may be null)
     * @return a non-null list
     */
    public static <T> List<T> emptyListIfNull(final List<T> list) {
        return Optional.ofNullable(list).orElse(Collections.emptyList());
    }

    /**
     * Returns an empty map if the given map is null, otherwise returns the map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map (may be null)
     * @return a non-null map
     */
    public static <K, V> Map<K, V> emptyMapIfNull(final Map<K, V> map) {
        return Optional.ofNullable(map).orElse(Collections.emptyMap());
    }

    /**
     * Checks if a string is null or blank.
     *
     * @param string the string (may be null)
     * @return {@code true} if the string is null or blank
     */
    public static boolean isEmpty(final String string) {
        return Optional.ofNullable(string)
                .map(String::isBlank)
                .orElse(true);
    }

    /**
     * Checks if a string is non-null and non-blank.
     *
     * @param string the string (may be null)
     * @return {@code true} if the string is non-null and not blank
     */
    public static boolean isNotEmpty(final String string) {
        return !isEmpty(string);
    }

    /**
     * Checks if a collection is null or empty.
     *
     * @param collection the collection (may be null)
     * @return {@code true} if the collection is null or empty
     */
    public static boolean isEmpty(final Collection<?> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::isEmpty)
                .orElse(true);
    }

    /**
     * Checks if a collection is non-null and non-empty.
     *
     * @param collection the collection (may be null)
     * @return {@code true} if the collection is non-null and not empty
     */
    public static boolean isNotEmpty(final Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * Checks if a map is null or empty.
     *
     * @param map the map (may be null)
     * @return {@code true} if the map is null or empty
     */
    public static boolean isEmpty(final Map<?, ?> map) {
        return Optional.ofNullable(map)
                .map(Map::isEmpty)
                .orElse(true);
    }

    /**
     * Checks if a map is non-null and non-empty.
     *
     * @param map the map (may be null)
     * @return {@code true} if the map is non-null and not empty
     */
    public static boolean isNotEmpty(final Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * Transforms a map by applying key and value mapping functions.
     *
     * @param <OK>        the original key type
     * @param <OV>        the original value type
     * @param <NK>        the new key type
     * @param <NV>        the new value type
     * @param map         the source map
     * @param keyMapper   function to transform keys
     * @param valueMapper function to transform values
     * @return a new map with transformed entries
     */
    public static <OK, OV, NK, NV> Map<NK, NV> transormMap(
            final Map<OK, OV> map,
            final Function<? super Map.Entry<OK, OV>, ? extends NK> keyMapper,
            final Function<? super Map.Entry<OK, OV>, ? extends NV> valueMapper
    ) {
        return transormMap(
                map,
                keyMapper,
                valueMapper,
                (v1, v2) -> v2
        );
    }

    /**
     * Transforms a map by applying key and value mapping functions with a merge function
     * for handling duplicate keys.
     *
     * @param <OK>          the original key type
     * @param <OV>          the original value type
     * @param <NK>          the new key type
     * @param <NV>          the new value type
     * @param map           the source map
     * @param keyMapper     function to transform keys
     * @param valueMapper   function to transform values
     * @param mergeFunction function to resolve merge conflicts
     * @return a new map with transformed entries
     */
    public static <OK, OV, NK, NV> Map<NK, NV> transormMap(
            final Map<OK, OV> map,
            final Function<? super Map.Entry<OK, OV>, ? extends NK> keyMapper,
            final Function<? super Map.Entry<OK, OV>, ? extends NV> valueMapper,
            final BinaryOperator<NV> mergeFunction
    ) {
        return emptyMapIfNull(map)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        keyMapper,
                        valueMapper,
                        mergeFunction,
                        LinkedHashMap::new
                ));
    }

    /**
     * Converts an object to an {@link Instant}, supporting {@link Instant},
     * {@link Number} (epoch millis), and {@link java.time.temporal.Temporal} types.
     *
     * @param obj  the object to convert
     * @param zone the time zone for parsing string representations
     * @return the converted instant
     */
    public static Instant toInstant(final Object obj,
                                    final String zone) {
        if (obj instanceof Instant i) {
            return i;
        }
        if (obj instanceof Number n) {
            return Instant.ofEpochMilli(n.longValue());
        }
        if (obj instanceof Temporal t) {
            return Instant.from(t);
        }
        return LocalDateTime.parse(obj.toString())
                .atZone(ZoneId.of(zone))
                .toInstant();
    }

    /**
     * Parses a time unit name into a {@link ChronoUnit}.
     * Accepts common names like "seconds", "minutes", "hours", "days", etc.
     *
     * @param name the unit name (case-insensitive)
     * @return the corresponding {@code ChronoUnit}, or {@code null} if unrecognized
     */
    public static ChronoUnit parseUnit(final String name) {
        return switch (name.toLowerCase()) {
            case "nanos", "nanoseconds" -> ChronoUnit.NANOS;
            case "micros", "microseconds" -> ChronoUnit.MICROS;
            case "millis", "milliseconds" -> ChronoUnit.MILLIS;
            case "seconds" -> ChronoUnit.SECONDS;
            case "minutes" -> ChronoUnit.MINUTES;
            case "hours" -> ChronoUnit.HOURS;
            case "days" -> ChronoUnit.DAYS;
            case "weeks" -> ChronoUnit.WEEKS;
            case "months" -> ChronoUnit.MONTHS;
            case "years" -> ChronoUnit.YEARS;
            default -> null;
        };
    }

    /**
     * Converts an object to a {@link Temporal}, supporting {@link Temporal},
     * {@link Number} (epoch millis), and string parsing.
     *
     * @param obj  the object to convert
     * @param zone the time zone for parsing
     * @return the converted temporal
     */
    public static Temporal toTemporal(final Object obj,
                                      final String zone) {
        if (obj instanceof Temporal t) {
            return t;
        }
        if (obj instanceof Number n) {
            return Instant.ofEpochMilli(n.longValue());
        }
        return LocalDateTime.parse(obj.toString())
                .atZone(ZoneId.of(zone))
                .toInstant();
    }

    /**
     * Subtracts a duration from a {@link Temporal}, handling {@link Instant} specially.
     *
     * @param t      the temporal value
     * @param amount the amount to subtract
     * @param unit   the time unit
     * @return the resulting temporal
     */
    public static Temporal subtractFromTemporal(final Temporal t,
                                                final long amount,
                                                final ChronoUnit unit) {
        if (t instanceof Instant i) {
            return i.minus(amount, unit);
        }
        return t.minus(amount, unit);
    }

    /**
     * Adds a duration to a {@link Temporal}, handling {@link Instant} specially.
     *
     * @param t      the temporal value
     * @param amount the amount to add
     * @param unit   the time unit
     * @return the resulting temporal
     */
    public static Temporal addToTemporal(final Temporal t,
                                         final long amount,
                                         final ChronoUnit unit) {
        if (t instanceof Instant i) {
            return i.plus(amount, unit);
        }
        return t.plus(amount, unit);
    }
}
