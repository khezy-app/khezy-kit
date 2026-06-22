package io.github.khezyapp.templates.plugin;

import io.github.khezyapp.templates.TemplateContext;
import io.github.khezyapp.templates.TemplateResult;

/**
 * SPI for hooking into the template resolution lifecycle.
 * <p>
 * All methods have default no-op implementations, so implementors only
 * need to override the hooks they care about.
 * <p>
 * Veto-capable hooks ({@link #beforeResolve}, {@link #beforeShellRun})
 * can abort the current operation by returning {@code false}.
 */
public interface Plugin {

    /**
     * Called before the resolver chain runs. Return {@code false} to
     * abort the entire resolution.
     *
     * @param ctx the template context
     * @return true to continue, false to block resolution
     */
    default boolean beforeResolve(final TemplateContext ctx) {
        return true;
    }

    /**
     * Called after the resolver chain completes.
     *
     * @param result the resolution result (immutable)
     */
    default void afterResolve(final TemplateResult result) {
    }

    /**
     * Called before a shell command is executed. Return {@code false} to
     * skip the command (the placeholder text is left in place).
     *
     * @param command the shell command string
     * @return true to allow execution, false to skip
     */
    default boolean beforeShellRun(final String command) {
        return true;
    }

    /**
     * Called after a shell command completes successfully. Can transform
     * the command output before it is substituted into the template.
     *
     * @param command the shell command that was run
     * @param output  the raw command output
     * @return the (possibly modified) output to substitute
     */
    default String afterShellRun(final String command,
                                 final String output) {
        return output;
    }

    /**
     * Called when a shell command fails with an exception.
     *
     * @param placeholder the original placeholder text (e.g. {@code !`cmd`})
     * @param error       the exception that occurred
     */
    default void onResolveError(final String placeholder,
                                final Exception error) {
    }
}
