package io.github.khezyapp.dynamicform.model;

import java.util.Objects;

/**
 * A single predicate evaluated against a dependency's value.
 * <p>
 * {@code value} is the operand: a scalar for most ops, a two-element list for {@code BETWEEN}, or a
 * regex pattern for {@code REGEX}. The {@code NOT} op means "not equal to".
 *
 * @param op    the comparison operator
 * @param value the operand, may be {@code null} (e.g. {@code EXISTS})
 */
public record Condition(Op op, Object value) {

    /**
     * Compact canonical constructor that rejects a null operator.
     */
    public Condition {
        op = Objects.requireNonNull(op, "op must not be null");
    }

    /**
     * Creates a condition.
     *
     * @param op    the operator
     * @param value the operand
     * @return a new condition
     */
    public static Condition of(final Op op,
                               final Object value) {
        return new Condition(op, value);
    }
}
