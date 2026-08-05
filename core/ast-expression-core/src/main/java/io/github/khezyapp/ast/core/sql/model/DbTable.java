package io.github.khezyapp.ast.core.sql.model;

import java.util.Objects;

/**
 * Represents a database table reference with optional schema and alias.
 *
 * @param name   the table name
 * @param schema the schema name (may be {@code null})
 * @param alias  the table alias (may be {@code null})
 */
public record DbTable(
    String name,
    String schema,
    String alias
) {
    /**
     * Creates a table reference with just a name (no schema or alias).
     *
     * @param name the table name
     * @return a new DbTable
     */
    public static DbTable of(final String name) {
        return new DbTable(name, null, null);
    }

    /**
     * Returns the fully qualified name (schema + "." + name), or just the name
     * if no schema is set.
     *
     * @return the qualified name
     */
    public String qualifiedName() {
        return Objects.nonNull(schema) && !schema.isBlank() ? schema + "." + name : name;
    }
}
