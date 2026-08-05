package io.github.khezyapp.dynamicform.model;

/**
 * The data type of a field's value after coercion.
 * <p>
 * This is the type the {@code Coercer} maps raw input into and the {@code Validator} checks against.
 * {@code null} on a field means the field carries no value (e.g. {@code NOTICE} or {@code BUTTON}
 * nodes).
 */
public enum ValueType {
    STRING,
    NUMBER,
    DECIMAL,
    BOOLEAN,
    DATE_TIME,
    ARRAY,
    OBJECT,
    ENUM,
    FILE
}
