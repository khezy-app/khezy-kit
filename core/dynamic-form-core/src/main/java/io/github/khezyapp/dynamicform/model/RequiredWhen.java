package io.github.khezyapp.dynamicform.model;

import java.util.Objects;

/**
 * Declares that a field becomes <strong>mandatory only while</strong> a dependency satisfies a
 * condition. Reuses the same {@link Condition} language as {@link Visibility}, so there is one
 * evaluation kit for both features.
 * <p>
 * Example — an EDD field that is required only for legal persons:
 * {@code new RequiredWhen("customerType", new Condition(Op.EQ, "LEGAL_PERSON"))}.
 *
 * @param when      the dot-path dependency to check (e.g. {@code "customerType"})
 * @param condition the condition that, while matching, makes the field required
 */
public record RequiredWhen(String when, Condition condition) {

    /**
     * Compact canonical constructor that rejects null components.
     */
    public RequiredWhen {
        when = Objects.requireNonNull(when, "when must not be null");
        condition = Objects.requireNonNull(condition, "condition must not be null");
    }

    /**
     * Creates a conditional-required rule.
     *
     * @param when      the dependency path
     * @param condition the triggering condition
     * @return a new rule
     */
    public static RequiredWhen of(final String when,
                                  final Condition condition) {
        return new RequiredWhen(when, condition);
    }
}
