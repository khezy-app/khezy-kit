package io.github.khezyapp.clone.strategy;

import io.github.khezyapp.clone.annotation.IgnoreClone;
import io.github.khezyapp.clone.api.CloneContext;
import io.github.khezyapp.clone.api.CloneStrategy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * A fallback cloning strategy using Java Reflection for standard POJOs.
 * <p>
 * This strategy traverses the class hierarchy to copy all declared fields.
 * It respects {@link IgnoreClone} annotations and skips {@code static} or
 * {@code transient} modifiers.
 * </p>
 */
public class ReflectionStrategy implements CloneStrategy {

    @Override
    public boolean support(final Class<?> clz) {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T copy(final T origin,
                      final CloneContext context) {
        try {
            final var clz = origin.getClass();
            final var constructor = clz.getDeclaredConstructor();
            constructor.setAccessible(true);
            final T copy = (T) constructor.newInstance();

            context.registerVisited(origin, copy);

            Class<?> current = clz;
            while (Objects.nonNull(current) && current != Object.class) {
                for (final var field : current.getDeclaredFields()) {
                    if (!eligibleField(field)) {
                        continue;
                    }
                    field.setAccessible(true);
                    final var value = context.proceed(field.get(origin));
                    field.set(copy, value);
                }
                current = current.getSuperclass();
            }
            return copy;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Determines if a field is eligible for cloning based on modifiers and annotations.
     *
     * @param field the field to inspect
     * @return true if the field should be cloned
     */
    private boolean eligibleField(final Field field) {
        final var mod = field.getModifiers();
        final var ignore = field.getDeclaredAnnotation(IgnoreClone.class);
        return Objects.isNull(ignore) &&
                !Modifier.isStatic(mod) &&
                !Modifier.isTransient(mod);
    }
}
