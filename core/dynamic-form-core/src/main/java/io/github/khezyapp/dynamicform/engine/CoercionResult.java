package io.github.khezyapp.dynamicform.engine;

/**
 * The outcome of a single coercion attempt.
 *
 * @param success whether coercion succeeded
 * @param value   the coerced value when successful, otherwise {@code null}
 * @param message the failure message when unsuccessful, otherwise {@code null}
 */
public record CoercionResult(boolean success, Object value, String message) {

    /**
     * Creates a successful result.
     *
     * @param value the coerced value
     * @return a success
     */
    public static CoercionResult success(final Object value) {
        return new CoercionResult(true, value, null);
    }

    /**
     * Creates a failure.
     *
     * @param message the failure message
     * @return a failure
     */
    public static CoercionResult failure(final String message) {
        return new CoercionResult(false, null, message);
    }
}
