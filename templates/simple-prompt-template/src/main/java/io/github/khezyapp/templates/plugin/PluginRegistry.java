package io.github.khezyapp.templates.plugin;

import io.github.khezyapp.templates.TemplateContext;
import io.github.khezyapp.templates.TemplateResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Registry that holds a list of {@link Plugin} instances and provides
 * {@code fire*} methods to dispatch lifecycle events.
 * <p>
 * Veto-capable methods ({@link #fireBeforeResolve}, {@link #fireBeforeShellRun})
 * short-circuit on the first {@code false} return. {@link #fireAfterShellRun}
 * pipes command output through all registered plugins in sequence.
 */
public final class PluginRegistry {

    private final List<Plugin> plugins;

    /**
     * Creates a new PluginRegistry with the given plugins.
     *
     * @param plugins list of plugins (will be defensively copied)
     */
    public PluginRegistry(final List<Plugin> plugins) {
        this.plugins = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(plugins)));
    }

    /**
     * Returns the registered plugins (unmodifiable).
     *
     * @return plugin list
     */
    public List<Plugin> plugins() {
        return plugins;
    }

    /**
     * Fires {@link Plugin#beforeResolve} on all registered plugins.
     * Short-circuits on the first {@code false} return.
     *
     * @param ctx the template context
     * @return true if all plugins allowed resolution, false if blocked
     */
    public boolean fireBeforeResolve(final TemplateContext ctx) {
        for (final var plugin : plugins) {
            if (!plugin.beforeResolve(ctx)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fires {@link Plugin#afterResolve} on all registered plugins.
     *
     * @param result the resolution result
     */
    public void fireAfterResolve(final TemplateResult result) {
        for (final var plugin : plugins) {
            plugin.afterResolve(result);
        }
    }

    /**
     * Fires {@link Plugin#beforeShellRun} on all registered plugins.
     * Short-circuits on the first {@code false} return.
     *
     * @param command the shell command
     * @return true if all plugins allowed execution, false if blocked
     */
    public boolean fireBeforeShellRun(final String command) {
        for (final var plugin : plugins) {
            if (!plugin.beforeShellRun(command)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fires {@link Plugin#afterShellRun} on all registered plugins,
     * piping the output through each plugin in sequence.
     *
     * @param command the shell command
     * @param output  the raw command output
     * @return the (possibly modified) output
     */
    public String fireAfterShellRun(final String command, final String output) {
        var result = output;
        for (final var plugin : plugins) {
            result = plugin.afterShellRun(command, result);
        }
        return result;
    }

    /**
     * Fires {@link Plugin#onResolveError} on all registered plugins.
     *
     * @param placeholder the placeholder text that failed
     * @param error       the exception that occurred
     */
    public void fireOnResolveError(final String placeholder, final Exception error) {
        for (final var plugin : plugins) {
            plugin.onResolveError(placeholder, error);
        }
    }
}
