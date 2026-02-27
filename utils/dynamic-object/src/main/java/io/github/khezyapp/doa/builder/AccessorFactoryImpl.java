package io.github.khezyapp.doa.builder;

import io.github.khezyapp.doa.adapters.CompositeTypeAdapter;
import io.github.khezyapp.doa.api.CollectionTypeAdapter;
import io.github.khezyapp.doa.api.ObjectAccessor;
import io.github.khezyapp.doa.api.PathParser;
import io.github.khezyapp.doa.api.TypeAdapter;
import io.github.khezyapp.doa.engine.DefaultObjectAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of {@link AccessorFactory} that facilitates the construction
 * of an {@link ObjectAccessor} using a fluent API.
 * <p>
 * This factory allows for the registration of multiple {@link TypeAdapter}s (for POJOs, Maps, Records)
 * and a specific {@link CollectionTypeAdapter} (for Lists/Arrays). The order in which adapters
 * are registered determines their priority during type resolution.
 * </p>
 */
public class AccessorFactoryImpl implements AccessorFactory {
    private final List<TypeAdapter> adapters = new ArrayList<>();
    private CollectionTypeAdapter collectionAdapter;
    private PathParser parser;

    /**
     * Registers a property\-based adapter to the factory.
     * <p>
     * Use this to add support for specific object types like standard Beans,
     * Java Records, or Map structures.
     * </p>
     *
     * @param adapter the {@link TypeAdapter} to register; must not be null
     * @return this factory instance for method chaining
     * @throws NullPointerException if the adapter is null
     */
    @Override
    public AccessorFactory registerAdapter(final TypeAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter cannot be null");
        this.adapters.add(adapter);
        return this;
    }

    /**
     * Registers the adapter responsible for handling indexed collection types.
     * <p>
     * This adapter will be used when the {@link ObjectAccessor} encounters
     * index\-based path tokens (e.g., {@code [0]}).
     * </p>
     *
     * @param adapter the {@link CollectionTypeAdapter} to register; must not be null
     * @return this factory instance for method chaining
     * @throws NullPointerException if the adapter is null
     */
    @Override
    public AccessorFactory registerAdapter(final CollectionTypeAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter cannot be null");
        this.collectionAdapter = adapter;
        return this;
    }

    /**
     * Configures the {@link PathParser} strategy to be used by the generated accessor.
     *
     * @param parser the parser responsible for converting string paths to tokens; must not be null
     * @return this factory instance for method chaining
     * @throws NullPointerException if the parser is null
     */
    @Override
    public AccessorFactory withParser(final PathParser parser) {
        Objects.requireNonNull(parser, "parser cannot be null");
        this.parser = parser;
        return this;
    }

    /**
     * Constructs a new {@link DefaultObjectAccessor} configured with the current
     * state of the factory.
     * <p>
     * Ensure that at least one parser and relevant adapters have been registered
     * before calling this method.
     * </p>
     *
     * @return a fully initialized {@link ObjectAccessor}
     */
    @Override
    public ObjectAccessor build() {
        return new DefaultObjectAccessor(
                parser,
                new CompositeTypeAdapter(adapters),
                collectionAdapter
        );
    }
}
