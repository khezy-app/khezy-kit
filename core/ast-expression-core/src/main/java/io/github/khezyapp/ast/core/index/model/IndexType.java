package io.github.khezyapp.ast.core.index.model;

/**
 * Enumerates the types of database indexes that can be planned.
 */
public enum IndexType {
    /** Standard B-tree index for aggregation queries. */
    AGGREGATION,
    /** Functional (expression-based) index. */
    FUNCTIONAL,
    /** GIN index for JSONB, full-text search, and array operations. */
    GIN
}
