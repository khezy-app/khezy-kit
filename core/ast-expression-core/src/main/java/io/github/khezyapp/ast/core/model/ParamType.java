package io.github.khezyapp.ast.core.model;

/**
 * Enumerates the expected types for function parameters.
 * <p>
 * Used in {@link ParamSpec} to define the data type expected by a function's
 * positional or named parameter. The special {@link #ANY} type is used when
 * any data type is acceptable.
 * </p>
 */
public enum ParamType {
    /** Boolean type. */
    BOOLEAN,
    /** Integer (long) type. */
    INTEGER,
    /** Float (double) type. */
    FLOAT,
    /** String type. */
    STRING,
    /** List type. */
    LIST,
    /** Map type. */
    MAP,
    /** Any type (accepts all). */
    ANY
}
