package io.github.khezyapp.dynamicform.engine;

/**
 * Thrown when a form schema cannot be resolved — a hard dependency cycle, a dangling (unresolvable)
 * dependency, or an ambiguous duplicate declaration where two declarations are simultaneously
 * visible for the same value slot.
 */
public class SchemaException extends RuntimeException {

    /**
     * Creates a schema error.
     *
     * @param message the problem description
     */
    public SchemaException(final String message) {
        super(message);
    }
}
