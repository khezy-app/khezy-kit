package io.github.khezyapp.pluginlib;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Central orchestrator for discovering, instantiating, and managing plugins
 * of a given service type {@code T}.
 * <p>
 * A {@code PluginManager} combines a {@link CompositePluginLoader} (which
 * discovers {@link PluginCandidate candidates}) with a {@link PluginStore}
 * (which persists installation metadata). It supports both eager and lazy
 * instantiation, version-aware lookups, and manual installation of external
 * candidates.
 * <p>
 * Obtain an instance via the fluent {@link Builder}:
 * <pre>{@code
 * PluginManager<MyPlugin> manager = PluginManager.of(MyPlugin.class)
 *     .classpath()
 *     .directory(Path.of("/plugins"))
 *     .build();
 * }</pre>
 *
 * @param <T> the plugin service type that all managed plugins implement
 */
public class PluginManager<T> implements AutoCloseable {

    private final CompositePluginLoader<T> loader;
    private final PluginStore store;

    private final ConcurrentMap<String, PluginCandidate<T>> candidates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, T> instances = new ConcurrentHashMap<>();
    private volatile boolean loaded;

    /**
     * Package-private constructor. Use {@link #of(Class)} and the
     * {@link Builder} to create instances.
     *
     * @param loader the composite loader that discovers plugin candidates
     * @param store  the store that persists installation metadata
     */
    PluginManager(final CompositePluginLoader<T> loader,
                  final PluginStore store) {
        this.loader = loader;
        this.store = store;
    }

    /**
     * Entry point for creating a {@link Builder} for a specific plugin type.
     *
     * @param <T>  the plugin service type
     * @param type the {@link Class} token representing the service type
     * @return a new {@link Builder} (never {@code null})
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public static <T> Builder<T> of(final Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        return new Builder<>(type);
    }

    /**
     * Discovers all plugins and instantiates every candidate immediately.
     * After this call all discovered instances are cached internally and
     * can be retrieved via {@link #getPlugins()}.
     *
     * @return an unmodifiable list of all instantiated plugins
     */
    public List<T> loadEager() {
        ensureLoaded();
        final var result = new ArrayList<T>();
        for (final var entry : candidates.entrySet()) {
            final var instance = instantiate(entry.getKey(), entry.getValue());
            result.add(instance);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Discovers all plugins and returns a lazy {@link Stream} that
     * instantiates each candidate on-demand as the stream is consumed.
     * The stream is backed by a snapshot of the candidates at the time
     * of the call.
     *
     * @return a lazy sequential stream of plugin instances
     */
    public Stream<T> loadLazy() {
        ensureLoaded();
        final var entrySnapshot = new ArrayList<>(candidates.entrySet());
        final var iterator = new Iterator<T>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < entrySnapshot.size();
            }

            @Override
            public T next() {
                final var entry = entrySnapshot.get(index++);
                return instantiate(entry.getKey(), entry.getValue());
            }
        };
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, 0),
                false);
    }

    /**
     * Returns all currently instantiated plugins. If {@link #loadEager()} or
     * {@link #loadLazy()} has not been called yet this returns an empty list.
     *
     * @return a list of previously instantiated plugin instances
     */
    public List<T> getPlugins() {
        if (!loaded) {
            return List.of();
        }
        return candidates.keySet().stream()
                .map(instances::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Retrieves the plugin with the given name. If multiple versions exist,
     * the highest version is returned.
     *
     * @param name the plugin name
     * @return an {@link Optional} containing the plugin instance, or empty
     *         if no plugin with that name was discovered
     */
    public Optional<T> get(final String name) {
        final var matches = candidates.entrySet().stream()
                .filter(e -> name.equals(e.getValue().name()))
                .toList();
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        if (matches.size() == 1) {
            return Optional.of(instantiate(matches.get(0).getKey(), matches.get(0).getValue()));
        }
        return matches.stream()
                .max(Comparator.comparing(e -> Version.parse(e.getValue().version())))
                .map(e -> instantiate(e.getKey(), e.getValue()));
    }

    /**
     * Retrieves the plugin with the exact given name and version.
     *
     * @param name    the plugin name
     * @param version the plugin version
     * @return an {@link Optional} containing the plugin instance, or empty
     *         if no matching plugin was discovered
     */
    public Optional<T> get(final String name,
                           final String version) {
        final var key = key(name, version);
        final var candidate = candidates.get(key);
        if (candidate == null) {
            return Optional.empty();
        }
        return Optional.of(instantiate(key, candidate));
    }

    /**
     * Returns all discovered versions of the plugin with the given name.
     *
     * @param name the plugin name
     * @return a list of matching plugin instances (never {@code null})
     */
    public List<T> getAll(final String name) {
        return candidates.entrySet().stream()
                .filter(e -> name.equals(e.getValue().name()))
                .map(e -> instantiate(e.getKey(), e.getValue()))
                .toList();
    }

    /**
     * Returns the underlying {@link PluginStore} that holds installation
     * metadata for all discovered and manually installed plugins.
     *
     * @return the plugin store (never {@code null})
     */
    public PluginStore getStore() {
        return store;
    }

    /**
     * Releases all resources: closes the composite loader, clears the
     * internal caches, and resets the loaded state. After this call the
     * manager should be discarded.
     */
    @Override
    public void close() {
        try {
            loader.close();
        } catch (final Exception e) {
            // ignore
        }
        candidates.clear();
        instances.clear();
        loaded = false;
    }

    /**
     * Manually installs a plugin candidate from an external source.
     * The candidate is added to both the internal candidate registry and
     * the underlying {@link PluginStore}.
     *
     * @param candidate   the plugin candidate to install
     * @param source      the origin of the plugin (e.g. file, URL, classpath)
     * @param displayName a human-readable display name for the plugin
     * @param vendor      the organisation or individual that created the plugin
     */
    public void install(final PluginCandidate<T> candidate,
                        final PluginSource source,
                        final String displayName,
                        final String vendor) {
        final var plugin = toInstalledPlugin(candidate, source, displayName, vendor);
        store.install(plugin);
        candidates.putIfAbsent(key(candidate.name(), candidate.version()), candidate);
    }

    private synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        final var discovered = loader.loadPlugins();
        for (final var candidate : discovered) {
            final var key = key(candidate.name(), candidate.version());
            if (candidates.putIfAbsent(key, candidate) == null) {
                store.install(toInstalledPlugin(candidate,
                        new PluginSource.ClasspathSource(),
                        candidate.name(),
                        ""));
            }
        }
        loaded = true;
    }

    private T instantiate(final String key,
                          final PluginCandidate<T> candidate) {
        return instances.computeIfAbsent(key, k -> candidate.newInstance());
    }

    private static InstalledPlugin toInstalledPlugin(final PluginCandidate<?> candidate,
                                                     final PluginSource source,
                                                     final String displayName,
                                                     final String vendor) {
        final var now = Instant.now();
        return new InstalledPlugin(
                candidate.name(),
                candidate.version(),
                displayName,
                "",
                vendor,
                source,
                true,
                now,
                now,
                Map.of());
    }

    private static String key(final String name,
                              final String version) {
        return name + ":" + version;
    }

    /**
     * Fluent builder for constructing a {@link PluginManager}.
     * <p>
     * Use {@link PluginManager#of(Class)} to obtain a builder, then chain
     * zero or more loader sources ({@link #classpath()}, {@link #directory(Path)},
     * {@link #loader(PluginLoader)}) and optionally a custom {@link #store(PluginStore)}
     * before calling {@link #build()}.
     *
     * @param <T> the plugin service type
     */
    public static class Builder<T> {

        private final Class<T> type;
        private final List<PluginLoader<T>> loaders = new ArrayList<>();
        private PluginStore store = new InMemoryPluginStore();

        Builder(final Class<T> type) {
            this.type = type;
        }

        /**
         * Adds classpath-based plugin discovery via {@link ServiceLoader}.
         *
         * @return this builder (for chaining)
         */
        public Builder<T> classpath() {
            loaders.add(new ServiceLoaderPluginLoader<>(type));
            return this;
        }

        /**
         * Adds directory-based plugin discovery (recursive by default).
         * Equivalent to {@code directory(dir, true)}.
         *
         * @param dir the directory to scan for JAR files
         * @return this builder (for chaining)
         */
        public Builder<T> directory(final Path dir) {
            return directory(dir, true);
        }

        /**
         * Adds directory-based plugin discovery with configurable recursion
         * and delegate-first prefixes.
         *
         * @param dir                   the directory to scan for JAR files
         * @param recursive             whether to scan subdirectories recursively
         * @param delegateFirstPrefixes class-name prefixes that should be
         *                              delegated to the parent class loader first
         * @return this builder (for chaining)
         */
        public Builder<T> directory(final Path dir,
                                    final boolean recursive,
                                    final String... delegateFirstPrefixes) {
            loaders.add(new DirectoryPluginLoader<>(type, dir, recursive, delegateFirstPrefixes));
            return this;
        }

        /**
         * Adds a custom {@link PluginLoader} to the list of loaders.
         *
         * @param custom a custom plugin loader implementation
         * @return this builder (for chaining)
         */
        public Builder<T> loader(final PluginLoader<T> custom) {
            loaders.add(custom);
            return this;
        }

        /**
         * Overrides the default (in-memory) {@link PluginStore} with a
         * custom implementation.
         *
         * @param store the plugin store to use
         * @return this builder (for chaining)
         */
        public Builder<T> store(final PluginStore store) {
            this.store = store;
            return this;
        }

        /**
         * Constructs the {@link PluginManager} with the configured loaders
         * and store. If no loaders were added an empty composite loader is
         * used.
         *
         * @return a new {@link PluginManager} instance
         */
        public PluginManager<T> build() {
            final CompositePluginLoader<T> composite;
            if (loaders.isEmpty()) {
                composite = new CompositePluginLoader<>(Collections.emptyList());
            } else {
                composite = new CompositePluginLoader<>(new ArrayList<>(loaders));
            }
            return new PluginManager<>(composite, store);
        }
    }

    /**
     * Internal value type representing a parsed semantic version
     * (major.minor.patch).
     */
    private record Version(int major,
                           int minor,
                           int patch) implements Comparable<Version> {

        /**
         * Parses a version string in the format {@code "major.minor.patch"}.
         * Missing or non-numeric segments default to {@code 0}.
         *
         * @param version the version string to parse
         * @return a new {@code Version} instance
         */
        static Version parse(final String version) {
            final var parts = version.split("\\.");
            final var major = parts.length > 0 ? tryParse(parts[0]) : 0;
            final var minor = parts.length > 1 ? tryParse(parts[1]) : 0;
            final var patch = parts.length > 2 ? tryParse(parts[2]) : 0;
            return new Version(major, minor, patch);
        }

        /**
         * Safely parses an integer from a string, returning {@code 0} on
         * {@link NumberFormatException}.
         */
        private static int tryParse(final String s) {
            try {
                return Integer.parseInt(s);
            } catch (final NumberFormatException e) {
                return 0;
            }
        }

        @Override
        public int compareTo(final Version other) {
            var cmp = Integer.compare(major, other.major);
            if (cmp != 0) {
                return cmp;
            }
            cmp = Integer.compare(minor, other.minor);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(patch, other.patch);
        }
    }
}
