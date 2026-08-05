package io.github.khezyapp.ast.core.index.model;

import java.util.Objects;

/**
 * Represents a filter condition in the index analysis model.
 *
 * @param field            the field expression being filtered
 * @param operator         the filter operator
 * @param valueIsExpression whether the filter value is an expression itself
 */
public record FilterCondition(
        FieldExpression field,
        FilterOperator operator,
        boolean valueIsExpression
) {
    public FilterCondition {
        Objects.requireNonNull(field);
        Objects.requireNonNull(operator);
    }

    /**
     * Creates a filter condition with a literal value.
     *
     * @param field    the field expression
     * @param operator the filter operator
     * @return a new filter condition
     */
    public static FilterCondition of(final FieldExpression field,
                                     final FilterOperator operator) {
        return new FilterCondition(field, operator, false);
    }

    /**
     * Creates a filter condition where the value is an expression.
     *
     * @param field    the field expression
     * @param operator the filter operator
     * @return a new filter condition with expression value
     */
    public static FilterCondition expressionValue(final FieldExpression field,
                                                  final FilterOperator operator) {
        return new FilterCondition(field, operator, true);
    }
}
