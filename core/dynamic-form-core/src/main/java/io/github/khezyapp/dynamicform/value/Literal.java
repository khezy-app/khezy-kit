package io.github.khezyapp.dynamicform.value;

/**
 * A plain, non-dynamic value.
 *
 * @param value the literal value
 */
public record Literal(Object value) implements Value {

    /**
     * Creates a literal.
     *
     * @param value the literal value
     * @return a new literal
     */
    public static Literal of(final Object value) {
        return new Literal(value);
    }

    @Override
    public Object unwrap() {
        return this.value;
    }
}
