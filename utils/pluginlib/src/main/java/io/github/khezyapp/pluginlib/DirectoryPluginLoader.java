package io.github.khezyapp.pluginlib;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.JarFile;

/**
 * A {@link PluginLoader} that scans a directory (optionally recursively)
 * for JAR files and discovers plugins via {@link ServiceLoader} within each JAR.
 * <p>
 * Each JAR is loaded with a dedicated {@link PluginClassLoader} so that plugins
 * are isolated from one another and from the application classpath. Version
 * metadata is resolved from {@link PluginInfo} annotations, falling back
 * to the {@code Plugin-Version} attribute in the JAR manifest, and finally
 * to {@code "1.0.0"}.
 *
 * @param <T> the plugin service type
 */
public class DirectoryPluginLoader<T> implements PluginLoader<T>, AutoCloseable {

    private static final String DEFAULT_VERSION = "1.0.0";

    private final Class<T> type;
    private final Path dir;
    private final boolean recursive;
    private final String[] delegateFirstPrefixes;
    private PluginClassLoader classLoader;

    /**
     * Creates a directory-based plugin loader.
     *
     * @param type                  the service interface or abstract class to load
     * @param dir                   the directory to scan for JAR files
     * @param recursive             whether to scan subdirectories recursively
     * @param delegateFirstPrefixes class-name prefixes that should be delegated
     *                              to the parent class loader first (defaults to
     *                              {@code "java."}, {@code "javax."}, {@code "jdk."},
     *                              {@code "sun."} when empty)
     */
    public DirectoryPluginLoader(final Class<T> type,
                                 final Path dir,
                                 final boolean recursive,
                                 final String... delegateFirstPrefixes) {
        this.type = type;
        this.dir = dir;
        this.recursive = recursive;
        this.delegateFirstPrefixes = delegateFirstPrefixes;
    }

    /**
     * Scans the configured directory for JAR files, loads them into a
     * dedicated {@link PluginClassLoader}, and discovers all service
     * providers of type {@code T} via {@link ServiceLoader}.
     * <p>
     * For each discovered provider the version is resolved in this order:
     * <ol>
     *   <li>{@link PluginInfo#version()} annotation value</li>
     *   <li>{@code Plugin-Version} attribute from the JAR manifest</li>
     *   <li>{@code "1.0.0"} as the fallback default</li>
     * </ol>
     *
     * @return the list of discovered plugin candidates (never {@code null})
     */
    @Override
    public List<PluginCandidate<T>> loadPlugins() {
        final var jars = findJars();
        final var urls = new ArrayList<URL>();
        for (final var jar : jars) {
            try {
                urls.add(jar.toUri().toURL());
            } catch (final IOException e) {
                throw new RuntimeException("Failed to convert JAR path to URL: " + jar, e);
            }
        }

        if (urls.isEmpty()) {
            return List.of();
        }

        classLoader = new PluginClassLoader(
                urls.toArray(new URL[0]),
                type.getClassLoader(),
                delegateFirstPrefixes);

        final var providers = ServiceLoader.load(type, classLoader).stream().toList();
        final var candidates = new ArrayList<PluginCandidate<T>>();

        for (final var provider : providers) {
            final var providerClass = provider.type();
            final var info = providerClass.getAnnotation(PluginInfo.class);
            final var name = info != null ? info.name() : providerClass.getSimpleName();
            final var version = info != null ? info.version() : resolveVersion(providerClass);
            candidates.add(new PluginCandidate<>(name, version, providerClass));
        }

        return candidates;
    }

    /**
     * Closes the underlying {@link PluginClassLoader}, releasing any resources
     * held by the loaded JAR files. After closing this loader can no longer
     * be used to load plugins.
     */
    @Override
    public void close() {
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (final IOException e) {
                // ignore close exception
            }
        }
    }

    /**
     * Walks the configured directory and collects all {@code .jar} file paths.
     */
    private List<Path> findJars() {
        try (var walk = Files.walk(dir, recursive ? Integer.MAX_VALUE : 1)) {
            return walk.filter(p -> p.toString().endsWith(".jar"))
                    .sorted()
                    .toList();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to walk directory: " + dir, e);
        }
    }

    private static String resolveVersion(final Class<?> providerClass) {
        final var protectionDomain = providerClass.getProtectionDomain();
        if (protectionDomain == null) {
            return DEFAULT_VERSION;
        }
        final var codeSource = protectionDomain.getCodeSource();
        if (codeSource == null) {
            return DEFAULT_VERSION;
        }
        final var location = codeSource.getLocation();
        if (location == null) {
            return DEFAULT_VERSION;
        }

        try (var jar = new JarFile(location.getFile())) {
            final var manifest = jar.getManifest();
            if (manifest != null) {
                final var attr = manifest.getMainAttributes()
                        .getValue("Plugin-Version");
                if (attr != null && !attr.isBlank()) {
                    return attr;
                }
            }
        } catch (final IOException e) {
            // ignore
        }
        return DEFAULT_VERSION;
    }
}
