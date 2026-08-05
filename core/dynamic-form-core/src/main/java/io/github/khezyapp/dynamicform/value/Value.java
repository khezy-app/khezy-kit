package io.github.khezyapp.dynamicform.value;

/**
 * Open value model (P11, deferred): a field value is either a plain {@link Literal} or a
 * {@link Binding} to an expression.
 * <p>
 * v1 resolves literals only; bindings can be added post-v1 as a new {@code ValueType} plus one
 * resolver without breaking the existing contract. The engine currently operates on raw
 * {@link Object} values; this abstraction documents the extension point.
 */
public sealed interface Value permits Literal, Binding {

    /**
     * Returns the concrete value of this entry.
     *
     * @return the literal value, or {@code null} while a binding remains unresolved
     */
    Object unwrap();
}
