package io.github.khezyapp.datamasker.api;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A stateful context object used during a single masking operation to manage recursive
 * traversal and prevent infinite loops.
 * <p>
 * This class serves as the coordination layer between the masking strategies and the
 * underlying data structure. It maintains a registry of visited objects to ensure that
 * circular references are handled safely by returning a previously processed
 * (masked) version of the object instead of re-processing it.
 * </p>
 * <p>Key behaviors include:</p>
 * <ul>
 * <li><b>Circular Dependency Protection:</b> Uses an {@link IdentityHashMap} to track
 * object instances based on reference equality ({@code ==}) rather than {@code equals()},
 * ensuring that self-referencing structures do not cause {@link StackOverflowError}.</li>
 * <li><b>Recursive Delegation:</b> Provides the {@code processMask} method which
 * strategies call to mask nested properties, allowing the context to decide whether
 * to return a cached result or invoke the {@link SensitiveMaskerStrategy}.</li>
 * <li><b>Result Mapping:</b> Maps original object instances to their masked
 * counterparts (typically {@code Map} or {@code Collection} instances), preserving
 * the object graph's topology in the output.</li>
 * </ul>
 *
 * @see SensitiveMaskerStrategy
 * @see IdentityHashMap
 */
public class SensitiveMaskerContext {
    private final SensitiveMaskerStrategy masker;
    private final Map<Object, Object> visited;

    public SensitiveMaskerContext(final SensitiveMaskerStrategy masker) {
        this.masker = masker;
        this.visited = new IdentityHashMap<>();
    }

    public void registerVisited(final Object key,
                                final Object value) {
        this.visited.put(key, value);
    }

    /**
     * Processes a payload through the internal strategy while checking for circular references.
     *
     * @param payload the object to process
     * @return the masked result
     */
    public Object processMask(final Object payload) {
        if (visited.containsKey(payload)) {
            return visited.get(payload);
        }
        return masker.mask(payload, this);
    }
}
