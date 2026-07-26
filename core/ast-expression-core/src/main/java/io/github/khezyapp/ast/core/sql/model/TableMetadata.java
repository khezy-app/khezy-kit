package io.github.khezyapp.ast.core.sql.model;

import java.util.Map;

/**
 * Metadata describing a database table including its fields and relationship links.
 *
 * @param name   the table name
 * @param schema the schema name (may be {@code null})
 * @param fields the fields indexed by field name
 * @param links  the relationship links indexed by link name
 */
public record TableMetadata(
        String name,
        String schema,
        Map<String, FieldMetadata> fields,
        Map<String, LinkMetadata> links
) {
}
