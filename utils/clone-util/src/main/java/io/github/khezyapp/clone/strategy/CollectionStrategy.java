package io.github.khezyapp.clone.strategy;

import io.github.khezyapp.clone.api.CloneContext;
import io.github.khezyapp.clone.api.CloneStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A cloning strategy for {@link Collection} types.
 * <p>
 * Attempts to instantiate the same collection class via reflection. If instantiation fails,
 * it provides safe fallbacks to {@link HashSet} for sets or {@link ArrayList} for other collections.
 * </p>
 */
public class CollectionStrategy implements CloneStrategy {

    @Override
    public boolean support(final Class<?> clz) {
        return Collection.class.isAssignableFrom(clz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T copy(final T origin,
                      final CloneContext context) {
        final Collection<Object> source = (Collection<Object>) origin;
        Collection<Object> dest;
        try {
            dest = source.getClass().getDeclaredConstructor().newInstance();
        } catch (final Exception e) {
            dest = origin instanceof Set<?> ? new HashSet<>() : new ArrayList<>();
        }

        context.registerVisited(origin, dest);

        for (final Object item : source) {
            final var value = context.proceed(item);
            dest.add(value);
        }

        return (T) dest;
    }
}
