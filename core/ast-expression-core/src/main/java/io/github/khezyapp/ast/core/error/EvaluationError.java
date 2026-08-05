package io.github.khezyapp.ast.core.error;

/**
 * Represents an error that occurred during expression evaluation.
 * <p>
 * Each error carries an {@link ErrorCode} (either standard or custom),
 * a descriptive message, and an optional source identifier (e.g., the
 * parameter name or AST location where the error originated).
 * </p>
 *
 * @param errorCode the error code
 * @param message   the human-readable error message
 * @param source    the source of the error (may be {@code null})
 */
public record EvaluationError(
        ErrorCode errorCode,
        String message,
        String source
) {

    /**
     * Creates an error without a source identifier.
     *
     * @param code    the error code
     * @param message the error message
     * @return a new evaluation error
     */
    public static EvaluationError of(final ErrorCode code,
                                     final String message) {
        return new EvaluationError(code, message, null);
    }

    /**
     * Creates an error with a source identifier.
     *
     * @param code    the error code
     * @param message the error message
     * @param source  the error source (e.g., parameter name)
     * @return a new evaluation error
     */
    public static EvaluationError of(final ErrorCode code,
                                     final String message,
                                     final String source) {
        return new EvaluationError(code, message, source);
    }
}
