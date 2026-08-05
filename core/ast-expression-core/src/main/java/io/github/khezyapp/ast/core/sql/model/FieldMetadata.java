package io.github.khezyapp.ast.core.sql.model;

/**
 * Metadata describing a single database column/field.
 *
 * @param name          the field name
 * @param dataType      the data type (e.g., "STRING", "INTEGER", "FLOAT", "TIMESTAMP")
 * @param nullable      whether the field can be null
 * @param isPrimaryKey  whether the field is a primary key
 * @param isForeignKey  whether the field is a foreign key
 */
public record FieldMetadata(
        String name,
        String dataType,
        boolean nullable,
        boolean isPrimaryKey,
        boolean isForeignKey
) {
}
