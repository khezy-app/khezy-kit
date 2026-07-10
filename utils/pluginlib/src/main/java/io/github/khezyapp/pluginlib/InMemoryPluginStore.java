package io.github.khezyapp.pluginlib;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, in-memory implementation of {@link PluginStore}.
 * <p>
 * All installed plugins are held in a {@link ConcurrentHashMap} keyed by
 * {@code "name:version"}. This store is intended for testing, development,
 * or lightweight single-JVM deployments. It does <em>not</em> persist data
 * across restarts.
 */
public class InMemoryPluginStore implements PluginStore {

    private final ConcurrentHashMap<String, InstalledPlugin> plugins = new ConcurrentHashMap<>();

    @Override
    public InstalledPlugin install(final InstalledPlugin plugin) {
        final var key = key(plugin.name(), plugin.version());
        plugins.put(key, plugin);
        return plugin;
    }

    @Override
    public void uninstall(final String name,
                          final String version) {
        plugins.remove(key(name, version));
    }

    @Override
    public List<InstalledPlugin> list() {
        return List.copyOf(plugins.values());
    }

    @Override
    public Optional<InstalledPlugin> get(final String name,
                                         final String version) {
        return Optional.ofNullable(plugins.get(key(name, version)));
    }

    @Override
    public List<InstalledPlugin> getAll(final String name) {
        return plugins.values().stream()
                .filter(p -> p.name().equals(name))
                .toList();
    }

    @Override
    public Optional<InstalledPlugin> getLatest(final String name) {
        return getAll(name).stream()
                .max(Comparator.comparing(
                        InstalledPlugin::version,
                        InMemoryPluginStore::compareVersions));
    }

    @Override
    public void setEnabled(final String name,
                           final String version,
                           final boolean enabled) {
        final var key = key(name, version);
        plugins.computeIfPresent(key, (k, existing) -> new InstalledPlugin(
                existing.name(),
                existing.version(),
                existing.displayName(),
                existing.description(),
                existing.vendor(),
                existing.source(),
                enabled,
                existing.installedAt(),
                Instant.now(),
                existing.attributes()));
    }

    @Override
    public void clear() {
        plugins.clear();
    }

    /**
     * Builds the internal map key for a plugin as {@code "name:version"}.
     */
    private static String key(final String name,
                              final String version) {
        return name + ":" + version;
    }

    /**
     * Compares two version strings using simple major.minor.patch parsing.
     * Non-numeric segments are treated as {@code 0}.
     *
     * @return a negative integer, zero, or a positive integer as the first
     *         version is less than, equal to, or greater than the second
     */
    private static int compareVersions(final String v1,
                                       final String v2) {
        final var p1 = v1.split("\\.");
        final var p2 = v2.split("\\.");
        for (var i = 0; i < 3; i++) {
            final var n1 = i < p1.length ? tryParse(p1[i]) : 0;
            final var n2 = i < p2.length ? tryParse(p2[i]) : 0;
            if (n1 != n2) {
                return Integer.compare(n1, n2);
            }
        }
        return 0;
    }

    /**
     * Safely parses an integer from a string, returning {@code 0} on failure.
     */
    private static int tryParse(final String s) {
        try {
            return Integer.parseInt(s);
        } catch (final NumberFormatException e) {
            return 0;
        }
    }
}
