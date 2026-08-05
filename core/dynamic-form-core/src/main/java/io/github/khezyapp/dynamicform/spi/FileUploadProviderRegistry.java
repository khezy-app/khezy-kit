package io.github.khezyapp.dynamicform.spi;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of named {@link FileUploadProvider} instances.
 * <p>
 * A {@code FILE} field's {@code file.uploadProvider} names an entry in this registry. When no name
 * is given (or the name is unknown) the registry falls back to a {@code defaultProvider} — the core
 * supplies an in-memory implementation by default so FILE fields work out of the box.
 */
public final class FileUploadProviderRegistry {

    private final Map<String, FileUploadProvider> providers = new ConcurrentHashMap<>();
    private final FileUploadProvider defaultProvider;

    /**
     * Creates a registry backed by the given fallback provider.
     *
     * @param defaultProvider the provider used when no named provider applies
     */
    public FileUploadProviderRegistry(final FileUploadProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    /**
     * Creates a registry whose fallback is the built-in {@link InMemoryFileUploadProvider}.
     *
     * @return a registry with the in-memory default
     */
    public static FileUploadProviderRegistry withDefaults() {
        return new FileUploadProviderRegistry(new InMemoryFileUploadProvider());
    }

    /**
     * Registers (or replaces) a provider under the given name.
     *
     * @param name     the lookup name referenced by {@code file.uploadProvider}
     * @param provider the provider implementation
     */
    public void register(final String name,
                         final FileUploadProvider provider) {
        this.providers.put(name, provider);
    }

    /**
     * Looks up a named provider.
     *
     * @param name the provider name
     * @return the registered provider, or empty if unknown
     */
    public Optional<FileUploadProvider> get(final String name) {
        return Optional.ofNullable(this.providers.get(name));
    }

    /**
     * Resolves the provider for a field: the named provider when it exists, otherwise the default.
     *
     * @param name the provider name from the schema, may be {@code null}
     * @return the provider to use, never {@code null}
     */
    public FileUploadProvider resolve(final String name) {
        if (Objects.nonNull(name) && this.providers.containsKey(name)) {
            return this.providers.get(name);
        }
        return this.defaultProvider;
    }
}
