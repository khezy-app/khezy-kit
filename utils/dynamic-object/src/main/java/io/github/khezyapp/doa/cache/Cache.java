package io.github.khezyapp.doa.cache;

import java.util.function.Function;

/**
 * A simple functional interface for a key-value cache.
 *
 * @param <T> the type of the cached objects
 */
public interface Cache<T> {

    T get(String key, Function<String, T> computeFunc);

    /**
     * Removes all entries from the cache.
     */
    void clear();
}
