package io.github.khezyapp.doa.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A thread-safe LRU (Least Recently Used) cache implementation based on {@link LinkedHashMap}.
 * <p>
 * This implementation automatically evicts the eldest entry when the size exceeds
 * the defined maximum entries.
 * </p>
 *
 * @param <T> the type of the cached objects
 */
public class MapCache<T> extends LinkedHashMap<String, T> implements Cache<T> {

    private static final int MAX_ENTRIES = 1000;

    private final int maxEntries;

    /**
     * Constructs a MapCache with the default capacity of 1000 entries.
     */
    public MapCache() {
        this.maxEntries = MAX_ENTRIES;
    }

    /**
     * Constructs a MapCache with a specific capacity.
     *
     * @param maxEntries the maximum number of entries allowed before eviction
     * @throws IllegalArgumentException if maxEntries is negative
     */
    public MapCache(final int maxEntries) {
        if (maxEntries < 0) {
            throw new IllegalArgumentException("maxEntries cannot be negative");
        }
        this.maxEntries = maxEntries;
    }

    /**
     * Thread-safe retrieval and computation of cache entries.
     *
     * @param key         the lookup key
     * @param computeFunc the function to produce the value if missing
     * @return the cached or newly computed value
     */
    @Override
    public T get(final String key,
                 final Function<String, T> computeFunc) {
        return computeIfAbsent(key, computeFunc);
    }

    /**
     * Thread-safe clearing of all cache entries.
     */
    @Override
    public void clear() {
        super.clear();
    }

    /**
     * Policy for removing the eldest entry when the size limit is reached.
     *
     * @param eldest the least recently accessed entry
     * @return true if the current size exceeds maxEntries
     */
    @Override
    protected boolean removeEldestEntry(final Map.Entry<String, T> eldest) {
        return size() > maxEntries;
    }
}
