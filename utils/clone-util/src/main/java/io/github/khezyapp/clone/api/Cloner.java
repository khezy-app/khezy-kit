package io.github.khezyapp.clone.api;

/**
 * Core interface for performing deep clone operations.
 * <p>
 * A Cloner orchestrates the cloning process by selecting the appropriate
 * {@link CloneStrategy} based on the object's type.
 * </p>
 */
public interface Cloner {

    /**
     * Creates a deep copy of the provided object.
     *
     * @param <T>    the type of the object
     * @param origin the original object to be cloned
     * @return a deep copy of the original object, or null if the input is null
     */
    <T> T deepClone(T origin);

    /**
     * Resolves the specialized cloning strategy for a specific class.
     *
     * @param clz the class of the object being cloned
     * @return the {@link CloneStrategy} capable of handling the specified class
     */
    CloneStrategy getCloneStrategy(Class<?> clz);
}
