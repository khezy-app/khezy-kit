package io.github.khezyapp.ast.core.index.model;

/**
 * Index metadata representing a plain, directly indexable column.
 *
 * @param columnName the column name
 */
public record PlainColumn(String columnName) implements ExpressionIndexMetadata {
}
