package io.github.khezyapp.ast.core.sql.model;

import java.util.Objects;

/**
 * Enumerates data types for schema field definitions.
 */
public enum DataType {
    /** String/text type. */
    STRING,
    /** Integer/numeric type. */
    INTEGER,
    /** Float/decimal type. */
    FLOAT,
    /** Boolean type. */
    BOOLEAN,
    /** Timestamp/date type. */
    TIMESTAMP;

    /**
     * Returns whether this type is numeric (INTEGER or FLOAT).
     *
     * @return {@code true} if numeric
     */
    public boolean isNumeric() {
        return this == INTEGER || this == FLOAT;
    }

    public static DataType fromString(final String s) {
        if (Objects.isNull(s)) {
            return STRING;
        }
        return switch (s.toUpperCase()) {
            case "INTEGER", "INT" -> INTEGER;
            case "FLOAT", "DOUBLE", "DECIMAL", "NUMERIC" -> FLOAT;
            case "BOOLEAN", "BOOL" -> BOOLEAN;
            case "TIMESTAMP", "DATETIME", "DATE" -> TIMESTAMP;
            default -> STRING;
        };
    }
}
