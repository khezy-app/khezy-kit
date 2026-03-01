package io.github.khezyapp.clone.api;

/**
 * Strategy interface for cloning specific types of objects.
 * <p>
 * Implementations define how to copy different categories of data,
 * such as Collections, Maps, Arrays, or standard POJOs.
 * </p>
 */
public interface CloneStrategy {

    /**
     * Checks if this strategy is capable of cloning the given class.
     *
     * @param clz the class to check
     * @return true if this strategy supports the class, false otherwise
     */
    boolean support(Class<?> clz);

    /**
     * Performs the copy operation for an object using the provided context.
     *
     * @param <T>     the type of the object
     * @param origin  the original object to copy
     * @param context the current {@link CloneContext} for tracking visited objects and nested cloning
     * @return a copy of the original object
     */
    <T> T copy(T origin, CloneContext context);
}
