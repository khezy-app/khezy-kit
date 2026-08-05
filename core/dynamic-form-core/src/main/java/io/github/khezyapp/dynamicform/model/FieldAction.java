package io.github.khezyapp.dynamicform.model;

import java.util.Map;
import java.util.Objects;

/**
 * A side-effect declared on a field (P12), e.g. {@code test-connection}, {@code fetch}, or
 * sub-resource load.
 * <p>
 * Actions are executed on demand through an
 * {@link io.github.khezyapp.dynamicform.spi.ActionHandler} named by {@code handler}; the engine's
 * resolution pass never runs them.
 *
 * @param type    a free-form action label (e.g. {@code "onClick"}, {@code "onLoad"})
 * @param handler the name of the {@code ActionHandler} in the registry
 * @param params  optional static parameters passed to the handler
 */
public record FieldAction(
        String type,
        String handler,
        Map<String, Object> params
) {

    /**
     * Compact canonical constructor that validates and normalises components.
     */
    public FieldAction {
        type = Objects.requireNonNull(type, "type must not be null");
        handler = Objects.requireNonNull(handler, "handler must not be null");
        params = Objects.nonNull(params) ? Map.copyOf(params) : Map.of();
    }

    /**
     * Creates an action without parameters.
     *
     * @param type    the action label
     * @param handler the handler name
     * @return a new action
     */
    public static FieldAction of(final String type,
                                 final String handler) {
        return new FieldAction(type, handler, null);
    }
}
