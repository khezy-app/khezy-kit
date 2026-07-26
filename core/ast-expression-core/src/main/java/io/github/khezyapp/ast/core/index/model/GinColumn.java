package io.github.khezyapp.ast.core.index.model;

/**
 * Index metadata representing a GIN-indexable column with its operator class.
 *
 * @param columnName    the column name
 * @param operatorClass the GIN operator class (e.g., "jsonb_ops", "tsvector_ops")
 */
public record GinColumn(String columnName, String operatorClass) implements ExpressionIndexMetadata {
}
