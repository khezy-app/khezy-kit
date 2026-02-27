package io.github.khezyapp.doa.engine;

import io.github.khezyapp.doa.adapters.CompositeTypeAdapter;
import io.github.khezyapp.doa.api.CollectionTypeAdapter;
import io.github.khezyapp.doa.api.ObjectAccessor;
import io.github.khezyapp.doa.api.PathParser;
import io.github.khezyapp.doa.model.IndexToken;
import io.github.khezyapp.doa.model.PathToken;
import io.github.khezyapp.doa.model.PropertyToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of {@link ObjectAccessor} that navigates object graphs
 * using parsed path tokens.
 * <p>
 * This class orchestrates the interaction between a {@link PathParser} and various
 * {@link io.github.khezyapp.doa.api.TypeAdapter} implementations to read and write values in nested structures,
 * including support for safe-navigation and index-based access.
 * </p>
 */
public class DefaultObjectAccessor implements ObjectAccessor {
    private final PathParser parser;
    private final CompositeTypeAdapter adapters;
    private final CollectionTypeAdapter collectionTypeAdapter;

    /**
     * Constructs a DefaultObjectAccessor with the necessary components for path resolution.
     *
     * @param parser                 the strategy used to break string paths into tokens
     * @param adapters               a list of adapters for handling POJOs, Maps, and Records
     * @param collectionTypeAdapter a adapters for handling Lists and Arrays
     */
    public DefaultObjectAccessor(final PathParser parser,
                                 final CompositeTypeAdapter adapters,
                                 final CollectionTypeAdapter collectionTypeAdapter) {
        this.parser = parser;
        this.adapters = adapters;
        this.collectionTypeAdapter = collectionTypeAdapter;
    }

    /**
     * Retrieves a value from the target object at the specified path.
     * <p>
     * Iterates through path tokens (properties or indices) and delegates access to
     * the appropriate adapter. Supports safe-navigation via {@code PropertyToken#safeAccess()}.
     * </p>
     *
     * @param target the root object to start navigation from
     * @param path   the string path (e.g., "user.address[0].city")
     * @return the resolved value, or null if the path is broken and safe-access is enabled
     * @throws RuntimeException if a property is missing and safe-access is disabled
     */
    @Override
    public Object get(final Object target,
                      final String path) {
        final var pathTokens = parser.parse(path);
        var current = target;

        for (final var token : pathTokens) {
            if (Objects.isNull(current)) {
                return null;
            }
            if (token instanceof PropertyToken propToken) {
                try {
                    current = getProperty(current, propToken.name());
                } catch (final Exception e) {
                    if (propToken.safeAccess()) {
                        return null;
                    }
                    throw e;
                }
            } else if (token instanceof IndexToken indexToken) {
                current = getProperty(current, indexToken.index());
            }
        }
        return current;
    }

    /**
     * Sets a value at the specified path within the target object.
     * <p>
     * Navigates the path recursively and updates the target property. For immutable
     * structures like Records, this may return a new object instance.
     * </p>
     *
     * @param target the root object
     * @param path   the string path to the property to be updated
     * @param value  the new value to assign
     * @return the modified target object (or a new instance if immutable)
     */
    @Override
    public Object set(final Object target,
                      final String path,
                      final Object value) {
        final var tokens = parser.parse(path);
        return setRecursive(target, tokens, 0, value);
    }

    @SuppressWarnings("unchecked")
    private Object setRecursive(final Object pcurrent,
                                final List<PathToken> tokens,
                                final int index,
                                final Object value) {
        var current = pcurrent;
        if (index == tokens.size()) {
            return value;
        }
        final var token = tokens.get(index);
        if (token instanceof PropertyToken propToken) {
            var child = getProperty(current, propToken.name());
            if (Objects.isNull(child)) {
                child = new HashMap<String, Object>();
            }
            final var updatedChild = setRecursive(child, tokens, index + 1, value);
            return setProperty(current, propToken.name(), updatedChild);
        }
        if (token instanceof IndexToken indexToken) {
            if (!(current instanceof List)) {
                current = new ArrayList<Object>();
            }

            final var list = (List<Object>) current;
            while (list.size() <= indexToken.index()) {
                list.add(null);
            }

            var child = list.get(indexToken.index());

            if (Objects.isNull(child) && tokens.size() > index + 1) {
                // create container for next token
                final var next = tokens.get(index + 1);
                if (next instanceof PropertyToken) {
                    child = new HashMap<String, Object>();
                } else if (next instanceof IndexToken) {
                    child = new ArrayList<>();
                }
                list.set(indexToken.index(), child);
            } else if (tokens.size() == index + 1) {
                // set the whole value
                list.set(indexToken.index(), value);
                return current;
            }

            final var updatedChild = setRecursive(child, tokens, index + 1, value);

            list.set(indexToken.index(), updatedChild);
            return current;
        }
        throw new IllegalArgumentException("Invalid token: " + token);
    }

    private Object setProperty(final Object target,
                               final String property,
                               final Object value) {
        if (adapters.supports(target)) {
            return adapters.setValue(target, property, value);
        }
        return target;
    }

    private Object getProperty(final Object target,
                               final String property) {
        if (adapters.supports(target)) {
            return adapters.getValue(target, property);
        }
        throw new IllegalArgumentException("Could not find TypeAdaptor class to support class: %s"
            .formatted(target.getClass().getSimpleName()));
    }

    private Object getProperty(final Object target,
                               final int index) {
        return collectionTypeAdapter.getValue(target, index);
    }
}
