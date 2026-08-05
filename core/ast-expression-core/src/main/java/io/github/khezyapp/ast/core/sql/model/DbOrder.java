package io.github.khezyapp.ast.core.sql.model;

/**
 * Represents an ORDER BY clause specifying a column and direction.
 *
 * @param column    the column to order by
 * @param direction the sort direction (ASC or DESC)
 */
public record DbOrder(
        DbColumn column,
        SortDirection direction
) {
}
