package io.github.khezyapp.ast.core.index.model;

/**
 * Index metadata representing a functionally transformed column (e.g., UPPER(col)).
 *
 * @param columnName        the base column name
 * @param transformFunction the transformation function name
 */
public record FunctionalColumn(String columnName, String transformFunction) implements ExpressionIndexMetadata {
}
