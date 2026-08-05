package io.github.khezyapp.ast.core.index.model;

import io.github.khezyapp.ast.core.CoreUtils;

import java.util.Objects;

/**
 * Represents a field expression that may be plain or functionally transformed.
 *
 * @param columnName        the column name
 * @param isFunctional      whether the expression uses a transform function
 * @param transformFunction the transform function name (empty if not functional)
 */
public record FieldExpression(
        String columnName,
        boolean isFunctional,
        String transformFunction
) {
    public FieldExpression {
        Objects.requireNonNull(columnName);
        Objects.requireNonNull(transformFunction);
    }

    /**
     * Creates a plain (non-functional) field expression.
     *
     * @param columnName the column name
     * @return a new field expression
     */
    public static FieldExpression plain(final String columnName) {
        return new FieldExpression(columnName, false, "");
    }

    /**
     * Creates a functional field expression with a transform.
     *
     * @param columnName        the column name
     * @param transformFunction the transform function name
     * @return a new functional field expression
     */
    public static FieldExpression functional(final String columnName,
                                             final String transformFunction) {
        return new FieldExpression(columnName, true, transformFunction);
    }

    /**
     * Returns whether this field expression has a resolvable column name.
     *
     * @return {@code true} if the column name is non-empty
     */
    public boolean isResolvable() {
        return CoreUtils.isNotEmpty(columnName);
    }
}
