package io.github.khezyapp.ast.core.sql;

/**
 * Enumerates SQL parameter placeholder rendering styles.
 * <ul>
 *   <li>{@link #INDEXED} — uses positional placeholder (?)</li>
 *   <li>{@link #NAMED} — uses named placeholders (:name)</li>
 *   <li>{@link #INLINED} — inlines literal values directly into the SQL</li>
 * </ul>
 */
public enum SqlRenderStyle {
    /** Positional placeholder style (e.g., {@code ?}). */
    INDEXED,
    /** Named placeholder style (e.g., {@code :param}). */
    NAMED,
    /** Values are inlined directly into the SQL string. */
    INLINED
}
