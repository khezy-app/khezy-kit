package io.github.khezyapp.ast.core.index.analyzer;

import io.github.khezyapp.ast.core.index.model.ExpressionIndexMetadata;
import io.github.khezyapp.ast.core.model.Node;

/**
 * Strategy interface for resolving an AST expression node into
 * {@link io.github.khezyapp.ast.core.index.model.ExpressionIndexMetadata}.
 * <p>
 * Implementations determine whether an expression is indexable and what
 * kind of index it requires (plain column, functional, GIN, or non-indexable).
 * </p>
 */
@FunctionalInterface
public interface ExpressionIndexResolver {

    /**
     * Resolves an expression node to index metadata.
     *
     * @param expressionNode the AST expression node
     * @return the resolved index metadata
     */
    ExpressionIndexMetadata resolve(Node expressionNode);
}
