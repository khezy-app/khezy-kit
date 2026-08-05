package io.github.khezyapp.ast.core.sql.model;

/**
 * Enumerates SQL JOIN types.
 */
public enum JoinType {
    /** INNER JOIN. */
    INNER,
    /** LEFT OUTER JOIN. */
    LEFT,
    /** RIGHT OUTER JOIN. */
    RIGHT,
    /** CROSS JOIN. */
    CROSS
}
