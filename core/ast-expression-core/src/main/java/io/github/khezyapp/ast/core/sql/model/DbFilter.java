package io.github.khezyapp.ast.core.sql.model;

/**
 * Represents a filter condition in a database query.
 * <p>
 * A filter consists of a source (column or expression), an operator, and
 * a value (literal, column reference, function expression, or subquery).
 * </p>
 *
 * @param source   the filter source (column or expression)
 * @param operator the filter operator (e.g., "=", ">", "IN", "CONTAINS")
 * @param value    the filter value
 */
public record DbFilter(
        FilterValue source,
        String operator,
        FilterValue value
) {
    /**
     * Creates a filter from a column reference.
     *
     * @param column   the column being filtered
     * @param operator the filter operator
     * @param value    the filter value
     * @return a new DbFilter
     */
    public static DbFilter of(final DbColumn column,
                              final String operator,
                              final FilterValue value) {
        return new DbFilter(new FilterValue.ColumnRef(column), operator, value);
    }
}
