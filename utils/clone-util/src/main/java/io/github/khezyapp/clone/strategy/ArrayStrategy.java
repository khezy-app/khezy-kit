package io.github.khezyapp.clone.strategy;

import io.github.khezyapp.clone.api.CloneContext;
import io.github.khezyapp.clone.api.CloneStrategy;

import java.lang.reflect.Array;

/**
 * A cloning strategy for Java Arrays.
 * <p>
 * Handles both primitive and object arrays by creating a new array instance
 * of the same component type and length, then recursively cloning each element.
 * </p>
 */
public class ArrayStrategy implements CloneStrategy {

    @Override
    public boolean support(final Class<?> clz) {
        return clz.isArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T copy(final T origin,
                      final CloneContext context) {
        final var length = Array.getLength(origin);
        final var copy = Array.newInstance(origin.getClass().getComponentType(), length);

        context.registerVisited(origin, copy);

        for (int i = 0; i < length; i++) {
            final Object value = Array.get(origin, i);
            Array.set(copy, i, context.proceed(value));
        }

        return (T) copy;
    }
}
