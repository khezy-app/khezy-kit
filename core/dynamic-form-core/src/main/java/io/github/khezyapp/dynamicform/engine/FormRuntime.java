package io.github.khezyapp.dynamicform.engine;

import io.github.khezyapp.dynamicform.model.FieldSchema;
import io.github.khezyapp.dynamicform.spi.ActionHandlerRegistry;
import io.github.khezyapp.dynamicform.spi.FileUploadProvider;
import io.github.khezyapp.dynamicform.spi.FileUploadProviderRegistry;
import io.github.khezyapp.dynamicform.spi.OptionsProviderRegistry;

import java.util.Objects;

/**
 * The extension-point wiring for a resolution pass: the registries of options providers, upload
 * providers, and action handlers. Immutable once constructed — reuse a single instance across calls.
 *
 * @param optionsRegistry the named {@code OptionsProvider}s
 * @param uploadRegistry  the named {@code FileUploadProvider}s plus the default fallback
 * @param actionRegistry  the named {@code ActionHandler}s
 */
public record FormRuntime(
        OptionsProviderRegistry optionsRegistry,
        FileUploadProviderRegistry uploadRegistry,
        ActionHandlerRegistry actionRegistry
) {

    /**
     * Creates a runtime with empty option/action registries and the in-memory upload default.
     *
     * @return a default runtime
     */
    public static FormRuntime defaults() {
        return new FormRuntime(OptionsProviderRegistry.empty(), FileUploadProviderRegistry.withDefaults(),
                ActionHandlerRegistry.empty());
    }

    /**
     * Resolves the upload provider for a FILE field — the named provider when declared, otherwise
     * the registry default.
     *
     * @param field the FILE field
     * @return the provider to use
     */
    public FileUploadProvider uploadProviderFor(final FieldSchema field) {
        final var spec = field.file();
        final var name = Objects.nonNull(spec) ? spec.uploadProvider() : null;
        return this.uploadRegistry.resolve(name);
    }
}
