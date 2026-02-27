package io.github.khezyapp.doa.api;

/**
 * The main entry point for the DOA library.
 * Responsible for navigating object graphs using string-based paths
 * (e.g., "user.orders[0].price") to retrieve or update values.
 */
public interface ObjectAccessor {

    /**
     * Retrieves a value from the target object at the specified path.
     *
     * @param target The root object to start navigation from.
     * @param path   The string path (e.g., "profile.address.city").
     * @return The value at the path, or null if the path is unreachable.
     */
    Object get(Object target, String path);

    /**
     * Updates or creates a value at the specified path.
     * <p>
     * Note: For immutable objects (like Records), this method returns a new
     * instance representing the updated state. For mutable objects, it returns
     * the modified target.
     *
     * @param target The root object to modify.
     * @param path   The string path to the property.
     * @param value  The new value to set.
     * @return The updated object (either the same instance or a new one for immutables).
     */
    Object set(Object target, String path, Object value);
}
