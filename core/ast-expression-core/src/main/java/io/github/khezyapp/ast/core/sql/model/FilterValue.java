package io.github.khezyapp.ast.core.sql.model;

import java.util.List;
import java.util.Map;

/**
 * Sealed interface for filter value types in a database filter condition.
 * <p>
 * A filter value can be a literal, a column reference, a subquery, or a
 * function expression.
 * </p>
 */
public sealed interface FilterValue
        permits FilterValue.Literal, FilterValue.ColumnRef, FilterValue.Subquery, FilterValue.FunctionExpr {

    /**
     * A literal value.
     *
     * @param value the literal value
     */
    record Literal(Object value) implements FilterValue {
    }

    /**
     * A reference to another column.
     *
     * @param column the referenced column
     */
    record ColumnRef(DbColumn column) implements FilterValue {
    }

    /**
     * A subquery.
     *
     * @param query the subquery model
     */
    record Subquery(DbQuery query) implements FilterValue {
    }

    /**
     * A function expression (e.g., UPPER, LOWER, COALESCE).
     *
     * @param function   the function name
     * @param args       the function arguments
     * @param qualifiers additional qualifiers
     */
    record FunctionExpr(String function,
                        List<FilterValue> args,
                        Map<String, Object> qualifiers
    ) implements FilterValue {
    }
}
