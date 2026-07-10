package io.github.khezyapp.pluginlib;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A {@link PluginLoader} that aggregates multiple child loaders and
 * deduplicates their results.
 * <p>
 * Each child loader is invoked in the order they were provided. If two
 * loaders return candidates with the same name, version, <em>and</em>
 * provider class, only the first occurrence is retained (insertion-order
 * is preserved).
 *
 * @param <T> the plugin service type
 */
public class CompositePluginLoader<T> implements PluginLoader<T>, AutoCloseable {

    private static final String KEY_SEPARATOR = ":";

    private final List<PluginLoader<T>> loaders;

    /**
     * Creates a composite loader that delegates to the given list of loaders.
     *
     * @param loaders the child loaders to delegate to (will be copied defensively)
     */
    public CompositePluginLoader(final List<PluginLoader<T>> loaders) {
        this.loaders = List.copyOf(loaders);
    }

    /**
     * Discovers plugins from all child loaders, deduplicating by
     * {@code name:version:providerClass}.
     *
     * @return the merged list of unique plugin candidates
     */
    @Override
    public List<PluginCandidate<T>> loadPlugins() {
        final var seen = new LinkedHashMap<String, PluginCandidate<T>>();

        for (final var loader : loaders) {
            final var candidates = loader.loadPlugins();
            for (final var candidate : candidates) {
                final var key = candidate.name()
                        + KEY_SEPARATOR + candidate.version()
                        + KEY_SEPARATOR + candidate.providerClass().getName();
                seen.putIfAbsent(key, candidate);
            }
        }

        return new ArrayList<>(seen.values());
    }

    /**
     * Closes all child loaders that implement {@link AutoCloseable}.
     * Exceptions during close are silently ignored.
     */
    @Override
    public void close() {
        for (final var loader : loaders) {
            if (loader instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (final Exception e) {
                    // ignore close exception
                }
            }
        }
    }
}
