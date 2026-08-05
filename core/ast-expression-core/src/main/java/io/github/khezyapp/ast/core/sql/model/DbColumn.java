package io.github.khezyapp.ast.core.sql.model;

/**
 * Represents a column reference qualified by its table and optional alias.
 *
 * @param table the parent table
 * @param name  the column name
 * @param alias the column alias (may be {@code null})
 */
public record DbColumn(
        DbTable table,
        String name,
        String alias
) {
    /**
     * Creates a column reference without an alias.
     *
     * @param table the parent table
     * @param name  the column name
     * @return a new DbColumn
     */
    public static DbColumn of(final DbTable table,
                              final String name) {
        return new DbColumn(table, name, null);
    }
}
