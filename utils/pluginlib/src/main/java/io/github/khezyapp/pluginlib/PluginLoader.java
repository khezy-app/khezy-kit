package io.github.khezyapp.pluginlib;

import java.nio.file.Path;
import java.util.List;

/**
 * Strategy interface for discovering {@link PluginCandidate plugins} of a given
 * service type {@code T}.
 * <p>
 * A {@code PluginLoader} is responsible for locating plugin implementations
 * (e.g. via {@link java.util.ServiceLoader}, scanning a directory of JARs, or
 * fetching from a remote repository) and returning them as a list of candidates
 * that can later be instantiated by a {@link PluginManager}.
 * <p>
 * This is a {@link FunctionalInterface}; implementors need only provide
 * {@link #loadPlugins()}. The interface also offers convenience static factory
 * methods for the most common discovery strategies.
 *
 * @param <T> the plugin service (interface or abstract class) that discovered
 *            candidates must implement
 */
@FunctionalInterface
public interface PluginLoader<T> {

    /**
     * Discovers all available plugin candidates for the service type {@code T}.
     *
     * @return a list of discovered candidates (never {@code null})
     */
    List<PluginCandidate<T>> loadPlugins();

    /**
     * Creates a loader that discovers plugins via {@link java.util.ServiceLoader}
     * on the application classpath. Each service provider is scanned for a
     * {@link PluginInfo} annotation to derive its name and version.
     *
     * @param <T>  the plugin service type
     * @param type the {@link Class} token representing the service type
     * @return a new {@link ServiceLoaderPluginLoader}
     */
    static <T> PluginLoader<T> serviceLoader(final Class<T> type) {
        return new ServiceLoaderPluginLoader<>(type);
    }

    /**
     * Creates a loader that scans one or more JAR files from a directory.
     * Each JAR is loaded with a dedicated {@link PluginClassLoader} and then
     * scanned via {@link java.util.ServiceLoader}.
     *
     * @param <T>                 the plugin service type
     * @param type                the {@link Class} token representing the service type
     * @param dir                 the directory to scan
     * @param recursive           whether to scan subdirectories recursively
     * @param delegateFirst       class-name prefixes that should be loaded by the
     *                            parent class loader first (defaults to
     *                            {@code "java.", "javax.", "jdk.", "sun."})
     * @return a new {@link DirectoryPluginLoader}
     */
    static <T> PluginLoader<T> directory(final Class<T> type,
                                         final Path dir,
                                         final boolean recursive,
                                         final String... delegateFirst) {
        return new DirectoryPluginLoader<>(type, dir, recursive, delegateFirst);
    }

    /**
     * Creates a composite loader that delegates to several child loaders and
     * deduplicates the results by name, version, and provider class.
     *
     * @param <T>     the plugin service type
     * @param loaders the list of child loaders to delegate to
     * @return a new {@link CompositePluginLoader}
     */
    static <T> PluginLoader<T> composite(final List<PluginLoader<T>> loaders) {
        return new CompositePluginLoader<>(loaders);
    }
}
