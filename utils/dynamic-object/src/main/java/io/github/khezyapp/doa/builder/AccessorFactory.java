package io.github.khezyapp.doa.builder;

import io.github.khezyapp.doa.api.CollectionTypeAdapter;
import io.github.khezyapp.doa.api.ObjectAccessor;
import io.github.khezyapp.doa.api.PathParser;
import io.github.khezyapp.doa.api.TypeAdapter;

/**
 * Factory and configuration builder for creating {@link ObjectAccessor} instances.
 */
public interface AccessorFactory {

    /**
     * Registers a custom adapter to the top of the resolution chain.
     * @param adapter The adapter to add.
     * @return The builder instance.
     */
    AccessorFactory registerAdapter(TypeAdapter adapter);

    AccessorFactory registerAdapter(CollectionTypeAdapter adapter);

    /**
     * Configures a custom parser (e.g., to change delimiters from "." to "/").
     * @param parser The custom path parser.
     * @return The builder instance.
     */
    AccessorFactory withParser(PathParser parser);

    /**
     * Finalizes configuration and returns a thread-safe ObjectAccessor.
     */
    ObjectAccessor build();
}
