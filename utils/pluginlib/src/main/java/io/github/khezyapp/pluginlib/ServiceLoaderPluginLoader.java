package io.github.khezyapp.pluginlib;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * A {@link PluginLoader} that discovers plugins via the standard Java
 * {@link ServiceLoader} mechanism on the application classpath.
 * <p>
 * Each service provider is introspected for a {@link PluginInfo} annotation.
 * If present, the annotation's {@code name} and {@code version} are used;
 * otherwise the provider class's simple name and {@code "1.0.0"} are used
 * as defaults.
 *
 * @param <T> the plugin service type
 */
public class ServiceLoaderPluginLoader<T> implements PluginLoader<T> {

    private final Class<T> type;

    /**
     * Creates a loader that will discover implementations of the given type.
     *
     * @param type the service interface or abstract class to load
     */
    public ServiceLoaderPluginLoader(final Class<T> type) {
        this.type = type;
    }

    @Override
    public List<PluginCandidate<T>> loadPlugins() {
        final var providers = ServiceLoader.load(type).stream().toList();
        final var candidates = new ArrayList<PluginCandidate<T>>();
        for (final var provider : providers) {
            final var providerClass = provider.type();
            final var info = providerClass.getAnnotation(PluginInfo.class);
            final var name = info != null ? info.name() : providerClass.getSimpleName();
            final var version = info != null ? info.version() : "1.0.0";
            candidates.add(PluginCandidate.fromProvider(name, version, provider));
        }
        return candidates;
    }
}
