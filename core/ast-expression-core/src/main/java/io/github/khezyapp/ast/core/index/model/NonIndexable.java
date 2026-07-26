package io.github.khezyapp.ast.core.index.model;

/**
 * Index metadata indicating that an expression cannot be indexed.
 *
 * @param expressionRepresentation a string representation of the non-indexable expression
 */
public record NonIndexable(String expressionRepresentation) implements ExpressionIndexMetadata {

    @Override
    public String columnName() {
        return expressionRepresentation;
    }
}
