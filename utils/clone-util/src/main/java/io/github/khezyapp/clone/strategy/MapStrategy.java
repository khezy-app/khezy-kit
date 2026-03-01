package io.github.khezyapp.clone.strategy;

import io.github.khezyapp.clone.api.CloneContext;
import io.github.khezyapp.clone.api.CloneStrategy;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A cloning strategy for {@link Map} types.
 * <p>
 * Replicates the map structure by creating a new map instance and recursively
 * cloning both keys and values to maintain a completely detached deep copy.
 * </p>
 */
public class MapStrategy implements CloneStrategy {

    @Override
    public boolean support(final Class<?> clz) {
        return Map.class.isAssignableFrom(clz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T copy(final T origin,
                      final CloneContext context) {
        final Map<Object, Object> source = (Map<Object, Object>) origin;
        Map<Object, Object> dest;
        try {
            dest = (Map<Object, Object>) origin.getClass().getDeclaredConstructor().newInstance();
        } catch (final Exception e) {
            dest = origin instanceof LinkedHashMap<?, ?> ? new LinkedHashMap<>() : new HashMap<>();
        }

        context.registerVisited(origin, dest);

        for (final var entry : source.entrySet()) {
            final var key = context.proceed(entry.getKey());
            final var value = context.proceed(entry.getValue());
            dest.put(key, value);
        }

        return (T) dest;
    }
}
