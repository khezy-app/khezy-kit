package io.github.khezyapp.ast.core.index.model;

/**
 * Sealed interface for the result of resolving an expression to an index metadata type.
 * <p>
 * An expression can resolve to a {@link PlainColumn}, a {@link FunctionalColumn}
 * (e.g., UPPER(col)), a {@link GinColumn} (for GIN-indexable expressions),
 * or {@link NonIndexable} if the expression cannot be indexed.
 * </p>
 */
public sealed interface ExpressionIndexMetadata
        permits PlainColumn, FunctionalColumn, GinColumn, NonIndexable {

    /**
     * Returns the column name associated with this metadata.
     *
     * @return the column name
     */
    String columnName();

    /**
     * Returns whether this expression is indexable.
     *
     * @return {@code true} if indexable
     */
    default boolean isIndexable() {
        return !(this instanceof NonIndexable);
    }

    /**
     * Returns whether this expression uses a functional transformation.
     *
     * @return {@code true} if functional
     */
    default boolean isFunctional() {
        return this instanceof FunctionalColumn;
    }

    /**
     * Returns whether this expression uses a GIN index.
     *
     * @return {@code true} if GIN
     */
    default boolean isGin() {
        return this instanceof GinColumn;
    }
}
