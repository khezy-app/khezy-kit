package io.github.khezyapp.doa.adapters;

import io.github.khezyapp.doa.api.TypeAdapter;
import io.github.khezyapp.doa.cache.Cache;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link TypeAdapter} for interacting with Java Records.
 * <p>
 * Since Records are immutable, {@code setValue} performs a copy-on-write operation
 * by recreating the record using its canonical constructor with the updated property.
 * </p>
 */
public class RecordAdapter implements TypeAdapter {
    private final Cache<RecordMeta> cache;

    /**
     * Constructs a RecordAdapter with a metadata cache.
     *
     * @param cache cache for storing {@link RecordMeta} keyed by class name
     */
    public RecordAdapter(final Cache<RecordMeta> cache) {
        this.cache = cache;
    }


    /**
     * Determines if the target object is a Java Record.
     *
     * @param target the object to check
     * @return true if the target is a Record
     */
    @Override
    public boolean supports(final Object target) {
        return target instanceof Record;
    }

    /**
     * Retrieves a value from a Record component via its accessor method.
     *
     * @param target   the record instance
     * @param property the name of the record component
     * @return the component value
     * @throws IllegalArgumentException if the property does not exist on the record
     */
    @Override
    public Object getValue(final Object target,
                           final String property) {
        final var clz = target.getClass();
        final var metadata = cache.get(clz.getName(), key -> new RecordMeta(clz));
        final var getter = metadata.accessors.get(property);
        if (Objects.isNull(getter)) {
            throw new IllegalArgumentException("Could not found method '%s' in %s"
                    .formatted(property, clz.getSimpleName()));
        }
        try {
            return getter.invoke(target);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * "Sets" a value on a Record by creating a new instance.
     * <p>
     * Iterates through all record components, retains existing values for unchanged
     * properties, and applies the new value to the specified property.
     * </p>
     *
     * @param target   the original record instance
     * @param property the name of the component to update
     * @param value    the new value
     * @return a new record instance with the updated value
     */
    @Override
    public Object setValue(final Object target,
                           final String property,
                           final Object value) {
        try {
            final var clz = target.getClass();
            final var metadata = cache.get(clz.getName(), key -> new RecordMeta(clz));
            final var args = new Object[metadata.components.length];
            for (var i = 0; i < metadata.components.length; i++) {
                final var name = metadata.components[i].getName();
                final var accessor = metadata.components[i].getAccessor();
                final var current = accessor.invoke(target);
                args[i] = name.equals(property) ? value : current;
            }
            return metadata.constructor.newInstance(args);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inner metadata class used to cache reflection components of a Record.
     */
    public static class RecordMeta {
        final RecordComponent[] components;
        final Constructor<?> constructor;
        final Map<String, Method> accessors = new HashMap<>();

        RecordMeta(final Class<?> clazz) {
            try {
                components = clazz.getRecordComponents();
                final var types = Arrays.stream(components)
                    .map(RecordComponent::getType)
                    .toArray(Class<?>[]::new);
                constructor = clazz.getDeclaredConstructor(types);
                constructor.setAccessible(true);

                for (final var rc : components) {
                    accessors.put(rc.getName(), rc.getAccessor());
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
