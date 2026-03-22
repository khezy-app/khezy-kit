package io.github.khezyapp.datamasker.api;


import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Strategy interface for masking sensitive data within different types of payloads.
 */
public interface SensitiveMaskerStrategy {

    /**
     * Determines if this strategy can handle the given payload.
     *
     * @param payload the object to check
     * @return true if compatible
     */
    boolean supports(Object payload);

    /**
     * Executes the masking logic on the payload.
     *
     * @param payload the object to process
     * @param context the current masking context tracking visited objects
     * @return the masked object
     */
    Object mask(Object payload, SensitiveMaskerContext context);

    default boolean isArray(final Object payload) {
        return Optional.ofNullable(payload)
                .map(Object::getClass)
                .map(Class::isArray)
                .orElse(false);
    }

    default boolean isCollection(final Object payload) {
        return payload instanceof Collection;
    }

    default boolean isMap(final Object payload) {
        return payload instanceof Map;
    }

    default boolean isPrimitive(final Class<?> clz) {
        return clz.isPrimitive() ||
                Integer.class.isAssignableFrom(clz) ||
                Long.class.isAssignableFrom(clz) ||
                Byte.class.isAssignableFrom(clz) ||
                Short.class.isAssignableFrom(clz) ||
                Boolean.class.isAssignableFrom(clz) ||
                Double.class.isAssignableFrom(clz) ||
                Float.class.isAssignableFrom(clz) ||
                Character.class.isAssignableFrom(clz) ||
                Number.class.isAssignableFrom(clz) ||
                String.class.isAssignableFrom(clz);
    }
}
