package io.github.khezyapp.dynamicform.engine;

import io.github.khezyapp.dynamicform.model.FieldSchema;

import java.util.Objects;

/**
 * Applies default values (P6) — defaults are always applied when a visible field has no value.
 */
public final class DefaultFiller {

    private DefaultFiller() {
    }

    /**
     * Returns the field's default when the current value is absent.
     *
     * @param field        the field
     * @param currentValue the current value, may be {@code null}
     * @return the current value if present, otherwise the field's default
     */
    public static Object fillIfAbsent(final FieldSchema field,
                                      final Object currentValue) {
        return Objects.isNull(currentValue) ? field.defaultValue() : currentValue;
    }
}
