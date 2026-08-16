package io.github.khezyapp.dhttp.spec;

import java.util.Objects;

/**
 * A precondition used to gate an {@link Operation} ({@code whens}). All fields are optional except
 * {@code property}; at most one of {@code equals} or {@code exists} is expected to be meaningful for
 * a given check.
 *
 * @param property the dotted path to read (resolved through {@code DynamicObjects})
 * @param equals   an expected literal value, or {@code null} whens not applicable
 * @param exists   "true"/"false" string asserting presence of {@code property}, or {@code null}
 */
public record Condition(String property, Object equals, String exists) {

    public Condition {
        Objects.requireNonNull(property, "property");
    }

    public Condition(final String property) {
        this(property, null, null);
    }

    public Condition(final String property,
                     final Object equals) {
        this(property, equals, null);
    }
}
