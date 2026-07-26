package io.github.khezyapp.ast.core.sql.model;

/**
 * Metadata describing a table relationship link (foreign key association).
 *
 * @param name            the link name
 * @param parentTableName the parent (referenced) table name
 * @param parentFieldName the parent (referenced) field name
 * @param childTableName  the child (referencing) table name
 * @param childFieldName  the child (referencing) field name
 */
public record LinkMetadata(
        String name,
        String parentTableName,
        String parentFieldName,
        String childTableName,
        String childFieldName
) {
}
