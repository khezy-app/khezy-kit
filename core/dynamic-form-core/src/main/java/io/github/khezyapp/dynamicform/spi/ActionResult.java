package io.github.khezyapp.dynamicform.spi;

import java.util.Map;

/**
 * The outcome of a declared {@link io.github.khezyapp.dynamicform.model.FieldAction} invocation.
 *
 * @param success whether the action completed successfully
 * @param message a human-readable result message
 * @param data    optional structured data returned by the action
 */
public record ActionResult(boolean success, String message, Map<String, Object> data) {

    /**
     * Creates a successful result.
     *
     * @param message the result message
     * @return a successful result
     */
    public static ActionResult ok(final String message) {
        return new ActionResult(true, message, Map.of());
    }

    /**
     * Creates a failed result.
     *
     * @param message the failure message
     * @return a failed result
     */
    public static ActionResult failed(final String message) {
        return new ActionResult(false, message, Map.of());
    }
}
