package io.github.khezyapp.dynamicform.engine;

import io.github.khezyapp.dynamicform.model.Condition;
import io.github.khezyapp.dynamicform.model.FieldSchema;
import io.github.khezyapp.dynamicform.model.Visibility;
import io.github.khezyapp.dynamicform.value.FormValues;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The single, side-effect-free visibility evaluator (P4).
 * <p>
 * {@code show} is an AND of named dependency predicates — every predicate must match; {@code hide}
 * is an OR — any match hides. Dependency names are dot-paths into the values, or the special context
 * references {@code @version}, {@code @deployment}, {@code @feature[:name]}. Unresolved values are
 * treated as "shown/unknown" (P11 deferral): a field is never force-hidden because a dependency has
 * not been evaluated yet.
 */
public final class VisibilityEvaluator {

    private VisibilityEvaluator() {
    }

    /**
     * Evaluates whether a field is visible at the top level.
     *
     * @param field  the field
     * @param values the current values
     * @param ctx    the evaluation context
     * @return {@code true} when visible
     */
    public static boolean isVisible(final FieldSchema field,
                                    final FormValues values,
                                    final EvalContext ctx) {
        return isVisible(field, values, ctx, "");
    }

    /**
     * Evaluates visibility within a nested scope (GROUP/COLLECTION row).
     *
     * @param field     the field
     * @param values    the current values
     * @param ctx       the evaluation context
     * @param scopePath the parent path prefix (e.g. {@code "documents"} or {@code "directors[2]"})
     * @return {@code true} when visible
     */
    public static boolean isVisible(final FieldSchema field,
                                    final FormValues values,
                                    final EvalContext ctx,
                                    final String scopePath) {
        final var visibility = field.visibility();
        if (Objects.isNull(visibility)) {
            return true;
        }
        return isShown(visibility, values, ctx, scopePath) && isNotHidden(visibility, values, ctx, scopePath);
    }

    /**
     * Evaluates a single condition against a value.
     *
     * @param condition the predicate
     * @param value     the dependency value, may be {@code null}
     * @return {@code true} when the condition holds
     */
    public static boolean matches(final Condition condition,
                                  final Object value) {
        if (Objects.isNull(value)) {
            return switch (condition.op()) {
                case EXISTS -> false;
                case EQ -> Objects.isNull(condition.value());
                case NOT -> Objects.nonNull(condition.value());
                default -> false;
            };
        }
        return switch (condition.op()) {
            case EQ -> equalsValue(value, condition.value());
            case NOT -> !equalsValue(value, condition.value());
            case GTE -> compare(value, condition.value()) >= 0;
            case LTE -> compare(value, condition.value()) <= 0;
            case GT -> compare(value, condition.value()) > 0;
            case LT -> compare(value, condition.value()) < 0;
            case BETWEEN -> between(value, condition.value());
            case STARTS_WITH -> value.toString().startsWith(String.valueOf(condition.value()));
            case ENDS_WITH -> value.toString().endsWith(String.valueOf(condition.value()));
            case INCLUDES -> includes(value, condition.value());
            case REGEX -> value.toString().matches(String.valueOf(condition.value()));
            case EXISTS -> true;
            default -> false;
        };
    }

    /**
     * Resolves a reference — either a dot-path into the values or a special {@code @} context key.
     *
     * @param reference the reference
     * @param values    the current values
     * @param ctx       the evaluation context
     * @param scopePath the nested scope prefix
     * @return the referenced value, or {@code null}
     */
    public static Object resolveReference(final String reference,
                                          final FormValues values,
                                          final EvalContext ctx, final String scopePath) {
        if (reference.startsWith("@")) {
            return resolveContextReference(reference, ctx);
        }
        if (scopePath.isEmpty()) {
            return values.get(reference);
        }
        final var scoped = scopePath + "." + reference;
        return values.has(scoped) ? values.get(scoped) : values.get(reference);
    }

    private static boolean isShown(final Visibility visibility,
                                   final FormValues values,
                                   final EvalContext ctx,
                                   final String scopePath) {
        for (final var entry : visibility.show().entrySet()) {
            final var value = resolveReference(entry.getKey(), values, ctx, scopePath);
            if (entry.getValue().stream().noneMatch(condition -> matches(condition, value))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNotHidden(final Visibility visibility,
                                       final FormValues values,
                                       final EvalContext ctx,
                                       final String scopePath) {
        for (final var entry : visibility.hide().entrySet()) {
            final var value = resolveReference(entry.getKey(), values, ctx, scopePath);
            if (entry.getValue().stream().anyMatch(condition -> matches(condition, value))) {
                return false;
            }
        }
        return true;
    }

    private static Object resolveContextReference(final String reference,
                                                  final EvalContext ctx) {
        if (reference.equals("@version")) {
            return ctx.schemaVersion();
        }
        if (reference.equals("@deployment")) {
            return ctx.deployment();
        }
        if (reference.equals("@feature")) {
            return ctx.features();
        }
        if (reference.startsWith("@feature:")) {
            return ctx.feature(reference.substring("@feature:".length()));
        }
        return null;
    }

    private static boolean equalsValue(final Object a,
                                       final Object b) {
        if (Objects.equals(a, b)) {
            return true;
        }
        if (a instanceof Number na && b instanceof Number nb) {
            return compare(na, nb) == 0;
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compare(final Object a,
                               final Object b) {
        if (a instanceof Number na && b instanceof Number nb) {
            return toBigDecimal(na).compareTo(toBigDecimal(nb));
        }
        if (a instanceof Comparable comparable && Objects.nonNull(b) && comparable.getClass().isInstance(b)) {
            return comparable.compareTo(b);
        }
        return a.toString().compareTo(String.valueOf(b));
    }

    private static BigDecimal toBigDecimal(final Number number) {
        if (number instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(number.toString());
    }

    private static boolean between(final Object value,
                                   final Object range) {
        if (!(range instanceof List<?> bounds) || bounds.size() != 2) {
            return false;
        }
        return compare(value, bounds.get(0)) >= 0 && compare(value, bounds.get(1)) <= 0;
    }

    private static boolean includes(final Object value,
                                    final Object needle) {
        if (value instanceof Collection<?> collection) {
            return collection.contains(needle);
        }
        if (value instanceof CharSequence text) {
            return text.toString().contains(String.valueOf(needle));
        }
        return false;
    }
}
