package io.github.khezyapp.doa.adapters;

import io.github.khezyapp.doa.api.TypeAdapter;

import java.util.Map;

/**
 * Implementation of {@link TypeAdapter} for interacting with {@link Map} objects.
 * <p>
 * Directly maps property names to Map keys.
 * </p>
 */
public class MapAdapter implements TypeAdapter {

    /**
     * Determines if the target object implements the {@link Map} interface.
     *
     * @param target the object to check
     * @return true if the target is a Map
     */
    @Override
    public boolean supports(final Object target) {
        return target instanceof Map;
    }

    /**
     * Retrieves a value from the map using the property name as the key.
     *
     * @param target   the map instance
     * @param property the key to look up
     * @return the value associated with the key
     */
    @Override
    public Object getValue(final Object target,
                           final String property) {
        return cast(target).get(property);
    }

    /**
     * Puts a value into the map using the property name as the key.
     *
     * @param target   the map instance
     * @param property the key to update
     * @param value    the value to set
     * @return the target map
     */
    @Override
    public Object setValue(final Object target,
                           final String property,
                           final Object value) {
        cast(target).put(property, value);
        return target;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(final Object value) {
        return (Map<String, Object>) value;
    }
}
