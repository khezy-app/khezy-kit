package io.github.khezyapp.datamasker;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

/**
 * A utility class providing high-performance reflection and introspection helpers
 * tailored for Java Bean manipulation.
 * <p>
 * This class simplifies common reflective tasks such as recursively searching for
 * fields in class hierarchies and retrieving {@link PropertyDescriptor} arrays
 * via standard Java Introspection.
 * </p>
 * * @see java.beans.Introspector
 * @see java.lang.reflect.Field
 */
public final class ReflectionUtils {

    private ReflectionUtils() {
    }

    /**
     * Recursively searches for a field with the specified name starting from
     * the given class and moving up through its superclasses.
     *
     * @param clazz     the class to start the search from
     * @param fieldName the name of the field to locate
     * @return the {@link Field} object if found, or {@code null} if the field
     * does not exist in the hierarchy or the class is null
     */
    public static Field findField(final Class<?> clazz,
                                  final String fieldName) {
        return doFindField(clazz, fieldName);
    }

    /**
     * Makes the provided field accessible by setting {@code setAccessible(true)}.
     * <p>
     * This allows the library to bypass Java language access control checks for
     * private or protected fields during the masking process.
     * </p>
     *
     * @param field the field to make accessible (null-safe)
     */
    public static void makeAccessible(final Field field) {
        if (Objects.nonNull(field)) {
            field.setAccessible(true);
        }
    }

    /**
     * Retrieves an array of {@link PropertyDescriptor} objects for the specified class
     * using the {@link Introspector}.
     * <p>
     * This is primarily used to identify "Getter" methods and their associated
     * property names in standard Java Beans.
     * </p>
     *
     * @param clazz the class to introspect
     * @return an array of property descriptors, or an empty array if an
     * {@link IntrospectionException} occurs or the class is null
     */
    public static PropertyDescriptor[] getPropertyDescriptors(final Class<?> clazz) {
        if (Objects.isNull(clazz)) {
            return null;
        }
        try {
            return Optional.ofNullable(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                    .orElse(new PropertyDescriptor[0]);
        } catch (final IntrospectionException ignored) {
            return new  PropertyDescriptor[0];
        }
    }

    private static Field doFindField(final Class<?> clazz,
                                     final String fieldName) {
        Class<?> searchType = clazz;
        while (Objects.nonNull(searchType)) {
            try {
                return searchType.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                searchType = searchType.getSuperclass();
            }
        }
        return null;
    }
}
