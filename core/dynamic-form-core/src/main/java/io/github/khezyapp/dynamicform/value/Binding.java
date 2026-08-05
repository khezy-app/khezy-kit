package io.github.khezyapp.dynamicform.value;

import java.util.Objects;

/**
 * A value bound to an expression (P11) — reserved for post-v1 support.
 *
 * @param expression the expression source
 */
public record Binding(String expression) implements Value {

    /**
     * Compact canonical constructor that rejects a null expression.
     */
    public Binding {
        expression = Objects.requireNonNull(expression, "expression must not be null");
    }

    @Override
    public Object unwrap() {
        return null;
    }
}
