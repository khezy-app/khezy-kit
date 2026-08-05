package io.github.khezyapp.dynamicform.spi;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of named {@link OptionsProvider} instances.
 * <p>
 * A field's {@code options.provider} names an entry in this registry. Registries are mutable and
 * shared — a consumer registers its providers once at startup and reuses the registry across
 * resolution calls.
 */
public final class OptionsProviderRegistry {

    private final Map<String, OptionsProvider> providers = new ConcurrentHashMap<>();

    private OptionsProviderRegistry() {
    }

    /**
     * Creates an empty registry.
     *
     * @return a new empty registry
     */
    public static OptionsProviderRegistry empty() {
        return new OptionsProviderRegistry();
    }

    /**
     * Registers (or replaces) a provider under the given name.
     *
     * @param name     the lookup name referenced by {@code options.provider}
     * @param provider the provider implementation
     */
    public void register(final String name, final OptionsProvider provider) {
        this.providers.put(name, provider);
    }

    /**
     * Looks up a provider by name.
     *
     * @param name the provider name
     * @return the registered provider, or empty if unknown
     */
    public Optional<OptionsProvider> get(final String name) {
        return Optional.ofNullable(this.providers.get(name));
    }
}
