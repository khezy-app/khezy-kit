package io.github.khezyapp.doa.adapters;

import io.github.khezyapp.doa.api.CollectionTypeAdapter;

import java.util.List;

/**
 * Implementation of {@link CollectionTypeAdapter} for interacting with {@link List} objects.
 * <p>
 * Provides index-based access to list elements while performing safety checks on bounds.
 * </p>
 */
public class ListAdapter implements CollectionTypeAdapter {

    /**
     * Determines if the target object implements the {@link List} interface.
     *
     * @param target the object to check
     * @return true if the target is a List
     */
    @Override
    public boolean supports(final Object target) {
        return target instanceof List<?>;
    }

    /**
     * Retrieves an element from the list at the specified index.
     *
     * @param target the list instance
     * @param index  the zero-based index
     * @return the element at the index, or null if the index is out of bounds
     */
    @Override
    public Object getValue(final Object target,
                           final int index) {
        final var list = (List<?>) target;
        if (index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    /**
     * Updates an element in the list at the specified index.
     *
     * @param target the list instance
     * @param index  the zero-based index
     * @param value  the value to set
     * @return the updated list, or null if the index is out of bounds
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object setValue(final Object target,
                           final int index,
                           final Object value) {
        final var list = (List<Object>) target;
        if (index >= list.size()) {
            return null;
        }
        list.set(index, value);
        return target;
    }
}
