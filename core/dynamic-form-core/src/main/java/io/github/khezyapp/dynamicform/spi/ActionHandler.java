package io.github.khezyapp.dynamicform.spi;

/**
 * Handles a declared {@link io.github.khezyapp.dynamicform.model.FieldAction side-effect} for a
 * field (test-connection, fetch, sub-resource load, …).
 * <p>
 * Actions are declared in the schema and executed on demand by the consumer (e.g. when the user
 * presses a {@code BUTTON} field) via the form engine — the engine's resolution pass itself never
 * triggers them.
 */
@FunctionalInterface
public interface ActionHandler {

    /**
     * Executes the action.
     *
     * @param context the action, owning field, current values, and evaluation context
     * @return the outcome of the action
     */
    ActionResult handle(ActionContext context);
}
