package io.github.khezyapp.doa.api;

/**
 * Defines how to read and write properties for a specific category of Java objects.
 * Implementations enable the library to be extended for custom types.
 */
public interface TypeAdapter {

    /**
     * Determines if this adapter can handle the given object instance.
     * @param target The object to check.
     * @return true if this adapter supports the object's type.
     */
    boolean supports(Object target);

    /**
     * Extracts a property value from the target.
     * @param target   The object to read from.
     * @param property The property name.
     * @return The property value.
     */
    Object getValue(Object target, String property);

    /**
     * Sets a property value on the target.
     * @param target   The object to modify.
     * @param property The property name.
     * @param value    The value to assign.
     * @return The resulting object (the original target or a new instance if immutable).
     */
    Object setValue(Object target, String property, Object value);

    /**
     * Removes a property from the target.
     * <p>
     * For Maps, this removes the key entirely. For POJOs and Records,
     * this sets the property to {@code null} (or the type's default value).
     * </p>
     * @param target   The object to modify.
     * @param property The property name to remove.
     * @return The resulting object (the original target or a new instance if immutable),
     *         or {@code null} if removal is not supported for this type.
     */
    Object removeValue(Object target, String property);
}
