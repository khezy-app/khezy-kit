package io.github.khezyapp.ast.core.error;

/**
 * Sealed interface representing an error code in the evaluation system.
 * <p>
 * {@code ErrorCode} distinguishes between predefined standard errors
 * ({@link Standard}) and user-defined custom errors ({@link Custom}).
 * Standard error codes are defined in {@link StandardErrors}.
 * </p>
 */
public sealed interface ErrorCode permits ErrorCode.Standard, ErrorCode.Custom {

    /**
     * Returns the unique error code string.
     *
     * @return the error code
     */
    String code();

    /**
     * Returns a human-readable description of the error.
     *
     * @return the error description
     */
    String description();

    /**
     * Creates a custom error code.
     *
     * @param code        the unique error code
     * @param description the error description
     * @return a new custom error code
     */
    static ErrorCode of(final String code,
                        final String description) {
        return new Custom(code, description);
    }

    /**
     * A predefined standard error code (see {@link StandardErrors}).
     *
     * @param code        the error code
     * @param description the error description
     */
    record Standard(String code, String description) implements ErrorCode {
    }

    /**
     * A user-defined custom error code.
     *
     * @param code        the error code
     * @param description the error description
     */
    record Custom(String code, String description) implements ErrorCode {
    }
}
