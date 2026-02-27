package io.github.khezyapp.doa.api;

/**
 * Defines how to read and write properties for a specific category of Java objects.
 * Implementations enable the library to be extended for custom types.
 */
public interface CollectionTypeAdapter {

    /**
     * Determines if this adapter can handle the given object instance.
     * @param target The object to check.
     * @return true if this adapter supports the object's type.
     */
    boolean supports(Object target);

    /**
     * Extracts a property value from the target.
     * @param target   The object to read from.
     * @param index The index of collection.
     * @return The property value.
     */
    Object getValue(Object target, int index);

    /**
     * Sets a property value on the target.
     * @param target   The object to modify.
     * @param index The index of collection.
     * @param value    The value to assign.
     * @return The resulting object (the original target or a new instance if immutable).
     */
    Object setValue(Object target, int index, Object value);
}
