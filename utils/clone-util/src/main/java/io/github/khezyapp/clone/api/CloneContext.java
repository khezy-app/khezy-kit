package io.github.khezyapp.clone.api;

import io.github.khezyapp.clone.annotation.IgnoreClone;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contextual state holder for a single deep clone operation.
 * <p>
 * Manages object identity to prevent infinite loops in circular graphs and
 * provides access to the cloner for recursive property processing.
 * </p>
 */
public class CloneContext {
    private final Map<Object, Object> visited;
    private final Cloner cloner;

    /**
     * Constructs a new CloneContext tied to a specific cloner.
     *
     * @param cloner the cloner to use for recursive calls
     */
    public CloneContext(final Cloner cloner) {
        this.cloner = cloner;
        this.visited = new IdentityHashMap<>();
    }

    /**
     * Registers an original object and its corresponding copy in the visited map.
     * <p>
     * This is essential for maintaining referential integrity in complex object graphs.
     * </p>
     *
     * @param original the source object
     * @param copy     the newly created copy
     */
    public void registerVisited(final Object original,
                                final Object copy) {
        visited.put(original, copy);
    }

    /**
     * Navigates deeper into the object graph to clone a property or element.
     * <p>
     * This method handles null checks, annotation\-based exclusions,
     * and circular reference resolution before delegating to the appropriate strategy.
     * </p>
     *
     * @param <T>    the type of the object
     * @param origin the object to process
     * @return the cloned instance, a previously cached copy, or null if ignored
     */
    @SuppressWarnings("unchecked")
    public <T> T proceed(final T origin) {
        if (Objects.isNull(origin) ||
                ignoreCloneClass(origin)) {
            return null;
        }
        if (visited.containsKey(origin)) {
            return (T) visited.get(origin);
        }
        final var strategy = cloner.getCloneStrategy(origin.getClass());
        return strategy.copy(origin, this);
    }

    /**
     * Checks if the object's class is marked with {@link IgnoreClone}.
     *
     * @param origin the object to inspect
     * @return true if the class should be skipped during cloning
     */
    private boolean ignoreCloneClass(final Object origin) {
        return Objects.nonNull(origin.getClass().getDeclaredAnnotation(IgnoreClone.class));
    }
}
