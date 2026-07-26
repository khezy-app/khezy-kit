package io.github.khezyapp.ast.core.sql.model;

/**
 * Represents an aggregation function applied to a column.
 *
 * @param function the aggregation function name (e.g., SUM, COUNT, AVG, MAX, MIN)
 * @param column   the target column
 * @param alias    the result alias
 * @param distinct whether to use DISTINCT
 */
public record DbAggregation(
        String function,
        DbColumn column,
        String alias,
        boolean distinct
) {
    /**
     * Creates an aggregation without DISTINCT.
     *
     * @param function the aggregation function name
     * @param column   the target column
     * @param alias    the result alias
     * @return a new DbAggregation
     */
    public static DbAggregation of(final String function,
                                   final DbColumn column,
                                   final String alias) {
        return new DbAggregation(function, column, alias, false);
    }
}
