package io.github.khezyapp.ast.core.model;

import java.util.Objects;

/**
 * A sealed identifier for a function in the expression AST.
 * <p>
 * {@code FunctionId} distinguishes between built-in core functions
 * ({@link Core}) and user-defined named functions ({@link Named}).
 * Use {@link #of(String)} to resolve a string value into the appropriate
 * variant.
 * </p>
 */
public sealed interface FunctionId permits FunctionId.Named, FunctionId.Core {

    /**
     * Returns the string representation of this function identifier.
     *
     * @return the function name
     */
    String value();

    /**
     * Returns whether this is a built-in core function.
     *
     * @return {@code true} if this is a {@link Core} function
     */
    boolean isCore();

    /**
     * Resolves a string value into a {@link FunctionId}.
     * <p>
     * If the value matches a known core function, a {@link Core} instance is returned;
     * otherwise a {@link Named} instance is created.
     * </p>
     *
     * @param value the function name (must not be null)
     * @return the resolved function identifier
     */
    static FunctionId of(final String value) {
        Objects.requireNonNull(value);
        return CoreFunctions.forValue(value).orElseGet(() -> new Named(value));
    }

    record Named(String value) implements FunctionId {

        public Named {
            Objects.requireNonNull(value);
        }

        public boolean isCore() {
            return false;
        }
    }

    record Core(String value) implements FunctionId {

        public Core {
            Objects.requireNonNull(value);
        }

        public boolean isCore() {
            return true;
        }
    }
}
