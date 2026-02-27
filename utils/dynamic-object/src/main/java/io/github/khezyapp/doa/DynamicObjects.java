package io.github.khezyapp.doa;

import io.github.khezyapp.doa.adapters.BeanAdapter;
import io.github.khezyapp.doa.adapters.ListAdapter;
import io.github.khezyapp.doa.adapters.MapAdapter;
import io.github.khezyapp.doa.adapters.RecordAdapter;
import io.github.khezyapp.doa.api.ObjectAccessor;
import io.github.khezyapp.doa.builder.AccessorFactoryImpl;
import io.github.khezyapp.doa.cache.MapCache;
import io.github.khezyapp.doa.engine.DefaultPathParser;

/**
 * The primary entry point for the Dynamic Objects library, providing a simplified
 * static API for object graph navigation and manipulation.
 * <p>
 * This class initializes a default {@link ObjectAccessor} configured with support for:
 * <ul>
 * <li>{@link java.util.Map} objects</li>
 * <li>Standard Java Beans (POJOs)</li>
 * <li>Java Records (with copy-on-write semantics)</li>
 * <li>{@link java.util.List} collections</li>
 * </ul>
 * All reflection metadata and path parsing results are cached internally to ensure
 * high-performance access.
 * </p>
 */
public final class DynamicObjects {
    private static final ObjectAccessor OBJECT_ACCESSOR;

    static {
        OBJECT_ACCESSOR = new AccessorFactoryImpl()
            .registerAdapter(new MapAdapter())
            .registerAdapter(new BeanAdapter(
                new MapCache<>(250),
                new MapCache<>(250)
            ))
            .registerAdapter(new RecordAdapter(new MapCache<>(250)))
            .registerAdapter(new ListAdapter())
            .withParser(new DefaultPathParser(new MapCache<>(250)))
            .build();
    }

    private DynamicObjects() {
    }

    /**
     * Retrieves a value from an object graph based on a string path.
     *
     * @param target the root object (Map, Record, List, or POJO)
     * @param path   the property path (e.g., "users[0].profile.name")
     * @return the resolved value, or null if the path is invalid or interrupted
     */
    public static Object get(final Object target,
                             final String path) {
        return OBJECT_ACCESSOR.get(target, path);
    }

    /**
     * Updates a value within an object graph at the specified path.
     * <p>
     * For immutable types like Java Records, this method will return a new instance
     * containing the updated value. For mutable types like Maps or POJOs, the
     * original object is modified and returned.
     * </p>
     *
     * @param target the root object
     * @param path   the property path to update
     * @param value  the new value to set
     * @return the updated object or a new instance (in the case of Records)
     */
    public static Object set(final Object target,
                             final String path,
                             final Object value) {
        return OBJECT_ACCESSOR.set(target, path, value);
    }
}
