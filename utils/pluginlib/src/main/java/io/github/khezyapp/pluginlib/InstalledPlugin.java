package io.github.khezyapp.pluginlib;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a plugin that has been installed into a {@link PluginStore}.
 * An {@code InstalledPlugin} carries metadata (name, version, vendor,
 * display name, description), lifecycle timestamps, the {@link PluginSource}
 * from which it originated, and an arbitrary key-value attribute map.
 * <p>
 * Once installed, a plugin can be enabled or disabled, and its metadata
 * is persisted by the owning store.
 *
 * @param name        unique plugin identifier (e.g. {@code "my-plugin"})
 * @param version     plugin version string (e.g. {@code "2.1.0"})
 * @param displayName human-readable display name shown in UIs
 * @param description free-text description of the plugin's purpose
 * @param vendor      organisation or individual that created the plugin
 * @param source      origin of the plugin (classpath, filesystem, URL)
 * @param enabled     whether the plugin is currently active
 * @param installedAt instant when the plugin was first installed
 * @param updatedAt   instant of the most recent metadata update
 * @param attributes  unmodifiable map of custom key-value metadata
 */
public record InstalledPlugin(
        String name,
        String version,
        String displayName,
        String description,
        String vendor,
        PluginSource source,
        boolean enabled,
        Instant installedAt,
        Instant updatedAt,
        Map<String, String> attributes) {

    /**
     * Compact canonical constructor that validates all components are non-{@code null}
     * and wraps the {@code attributes} map in an unmodifiable view.
     */
    public InstalledPlugin {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(vendor, "vendor must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(installedAt, "installedAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");
        attributes = Collections.unmodifiableMap(attributes);
    }
}
