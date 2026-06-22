package io.github.khezyapp.templates.plugin;

/**
 * Enumeration of all possible plugin lifecycle events.
 */
public enum PluginEvent {
    BEFORE_RESOLVE,
    AFTER_RESOLVE,
    BEFORE_SHELL_RUN,
    AFTER_SHELL_RUN,
    ON_RESOLVE_ERROR
}
