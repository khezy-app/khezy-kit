package io.github.khezyapp.dynamicform.model;

/**
 * Comparison operators used by visibility ({@link Condition}) and conditional-required
 * ({@link RequiredWhen}). They form the single evaluation kit shared by both features.
 */
public enum Op {
    EQ,
    NOT,
    GTE,
    LTE,
    GT,
    LT,
    BETWEEN,
    STARTS_WITH,
    ENDS_WITH,
    INCLUDES,
    REGEX,
    EXISTS
}
