package io.github.khezyapp.dynamicform.engine;

import io.github.khezyapp.dynamicform.model.Constraints;
import io.github.khezyapp.dynamicform.model.FieldIssue;
import io.github.khezyapp.dynamicform.model.FieldSchema;
import io.github.khezyapp.dynamicform.model.Options;
import io.github.khezyapp.dynamicform.model.RequiredWhen;
import io.github.khezyapp.dynamicform.spi.Option;
import io.github.khezyapp.dynamicform.value.FormValues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.PatternSyntaxException;

/**
 * Aggregates {@link FieldIssue}s for a coerced value (P7) — missing-required (including
 * conditional-required G3), bounds, length, pattern, expression, and inline-option membership. The
 * same shape the backend rejects a submit with is what the UI renders inline.
 */
public final class Validator {

    private Validator() {
    }

    /**
     * Validates a value at the top level.
     *
     * @param field  the field
     * @param value  the coerced value
     * @param values the current values (for conditional-required)
     * @param ctx    the evaluation context
     * @return the collected issues, never {@code null}
     */
    public static List<FieldIssue> validate(final FieldSchema field,
                                            final Object value,
                                            final FormValues values,
                                            final EvalContext ctx) {
        return validate(field, value, values, ctx, field.name(), "");
    }

    /**
     * Validates a value within a nested scope.
     *
     * @param field     the field
     * @param value     the coerced value
     * @param values    the current values (for conditional-required)
     * @param ctx       the evaluation context
     * @param path      the full issue path (e.g. {@code "directors[2].idNumber"})
     * @param scopePath the nested scope prefix
     * @return the collected issues, never {@code null}
     */
    public static List<FieldIssue> validate(final FieldSchema field,
                                            final Object value,
                                            final FormValues values,
                                            final EvalContext ctx,
                                            final String path,
                                            final String scopePath) {
        final var issues = new ArrayList<FieldIssue>();
        final var constraints = field.constraints();
        if (isRequired(constraints, values, ctx, scopePath) && isEmpty(value)) {
            issues.add(FieldIssue.error(path, "is required"));
            return issues;
        }
        validateInlineOptions(field, value, path, issues);
        if (Objects.isNull(constraints)) {
            return issues;
        }
        validateBounds(constraints, value, path, issues);
        validateLength(constraints, value, path, issues);
        validatePattern(constraints, value, path, issues);
        validateExpression(constraints, value, path, issues);
        return issues;
    }

    private static boolean isRequired(final Constraints constraints,
                                      final FormValues values,
                                      final EvalContext ctx,
                                      final String scopePath) {
        if (Objects.isNull(constraints)) {
            return false;
        }
        if (constraints.required()) {
            return true;
        }
        for (final RequiredWhen requiredWhen : constraints.requiredWhen()) {
            final var dependency = VisibilityEvaluator.resolveReference(
                    requiredWhen.when(),
                    values,
                    ctx,
                    scopePath
            );
            if (VisibilityEvaluator.matches(requiredWhen.condition(), dependency)) {
                return true;
            }
        }
        return false;
    }

    private static void validateBounds(final Constraints constraints,
                                       final Object value,
                                       final String path,
                                       final List<FieldIssue> issues) {
        if (value instanceof Number number) {
            if (constraints.min() instanceof Number min && number.doubleValue() < min.doubleValue()) {
                issues.add(FieldIssue.error(path, "must be >= " + min));
            }
            if (constraints.max() instanceof Number max && number.doubleValue() > max.doubleValue()) {
                issues.add(FieldIssue.error(path, "must be <= " + max));
            }
        }
    }

    private static void validateLength(final Constraints constraints,
                                       final Object value,
                                       final String path,
                                       final List<FieldIssue> issues) {
        if (!(value instanceof String text)) {
            return;
        }
        if (Objects.nonNull(constraints.minLength()) && text.length() < constraints.minLength()) {
            issues.add(FieldIssue.error(path, "must be at least " + constraints.minLength() + " characters"));
        }
        if (Objects.nonNull(constraints.maxLength()) && text.length() > constraints.maxLength()) {
            issues.add(FieldIssue.error(path, "must be at most " + constraints.maxLength() + " characters"));
        }
    }

    private static void validatePattern(final Constraints constraints,
                                        final Object value,
                                        final String path,
                                        final List<FieldIssue> issues) {
        if (Objects.isNull(constraints.pattern()) || !(value instanceof String text)) {
            return;
        }
        try {
            if (!text.matches(constraints.pattern())) {
                issues.add(FieldIssue.error(path, "does not match pattern " + constraints.pattern()));
            }
        } catch (final PatternSyntaxException e) {
            issues.add(FieldIssue.error(path, "invalid pattern in schema: " + constraints.pattern()));
        }
    }

    private static void validateExpression(final Constraints constraints,
                                           final Object value,
                                           final String path,
                                           final List<FieldIssue> issues) {
        if (constraints.noExpression() && value instanceof String text && text.contains("${")) {
            issues.add(FieldIssue.error(path, "expressions are not allowed"));
        }
    }

    private static void validateInlineOptions(final FieldSchema field,
                                              final Object value,
                                              final String path,
                                              final List<FieldIssue> issues) {
        final var options = field.options();
        if (Objects.isNull(options) || options.inline().isEmpty()) {
            return;
        }
        if (value instanceof List<?> list) {
            for (final Object item : list) {
                validateOptionMatch(options, item, path, issues);
            }
            return;
        }
        validateOptionMatch(options, value, path, issues);
    }

    private static void validateOptionMatch(final Options options,
                                            final Object value,
                                            final String path,
                                            final List<FieldIssue> issues) {
        final var allowed = options.inline().stream().map(Option::value).toList();
        if (!allowed.contains(String.valueOf(value))) {
            issues.add(FieldIssue.error(path, "is not a valid option"));
        }
    }

    private static boolean isEmpty(final Object value) {
        if (Objects.isNull(value)) {
            return true;
        }
        if (value instanceof String text) {
            return text.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        if (value instanceof byte[] bytes) {
            return bytes.length == 0;
        }
        return false;
    }
}
