package io.github.khezyapp.dynamicform.spi;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of named {@link ActionHandler} instances.
 * <p>
 * A field's {@code actions} reference handlers by name through this registry.
 */
public final class ActionHandlerRegistry {

    private final Map<String, ActionHandler> handlers = new ConcurrentHashMap<>();

    private ActionHandlerRegistry() {
    }

    /**
     * Creates an empty registry.
     *
     * @return a new empty registry
     */
    public static ActionHandlerRegistry empty() {
        return new ActionHandlerRegistry();
    }

    /**
     * Registers (or replaces) a handler under the given name.
     *
     * @param name    the lookup name referenced by a {@code FieldAction.handler}
     * @param handler the handler implementation
     */
    public void register(final String name,
                         final ActionHandler handler) {
        this.handlers.put(name, handler);
    }

    /**
     * Looks up a handler by name.
     *
     * @param name the handler name
     * @return the registered handler, or empty if unknown
     */
    public Optional<ActionHandler> get(final String name) {
        return Optional.ofNullable(this.handlers.get(name));
    }
}
