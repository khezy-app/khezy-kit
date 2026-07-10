package io.github.khezyapp.pluginlib;

import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Represents a discovered plugin that can be instantiated but has not yet been loaded.
 * A {@code PluginCandidate} pairs a human-readable name and version with the
 * concrete {@link Class} that implements the plugin contract. It is produced by a
 * {@link PluginLoader} and consumed by a {@link PluginManager} to create live instances.
 *
 * @param <T>           the plugin service type that this candidate implements
 * @param name          human-readable plugin name (e.g. {@code "my-plugin"})
 * @param version       plugin version string (e.g. {@code "2.1.0"})
 * @param providerClass the concrete class that implements {@code T}
 */
public record PluginCandidate<T>(
        String name,
        String version,
        Class<? extends T> providerClass) {

    /**
     * Compact canonical constructor that validates all components are non-{@code null}.
     */
    public PluginCandidate {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(providerClass, "providerClass must not be null");
    }

    /**
     * Creates a {@code PluginCandidate} from a {@link ServiceLoader.Provider},
     * extracting the provider class metadata via {@link ServiceLoader.Provider#type()}.
     *
     * @param <T>      the plugin service type
     * @param name     human-readable plugin name
     * @param version  plugin version string
     * @param provider the {@code ServiceLoader.Provider} from which the implementation
     *                 class is derived
     * @return a new {@code PluginCandidate} backed by the given provider's type
     */
    public static <T> PluginCandidate<T> fromProvider(
            final String name,
            final String version,
            final ServiceLoader.Provider<T> provider) {
        return new PluginCandidate<>(name, version, provider.type());
    }

    /**
     * Creates a fresh instance of the plugin by invoking the no-argument constructor
     * of {@code providerClass}. The constructor is made accessible if necessary.
     *
     * @return a new instance of the plugin implementation
     * @throws RuntimeException if the no-argument constructor cannot be found or
     *                          invocation fails
     */
    public T newInstance() {
        try {
            final var ctor = providerClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Failed to instantiate plugin: " + name
                            + ":" + version
                            + " (" + providerClass.getName() + ")",
                    e);
        }
    }
}
