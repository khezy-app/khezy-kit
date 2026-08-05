package io.github.khezyapp.dynamicform.model;

/**
 * The render (widget) type of a field.
 * <p>
 * Render type is <strong>orthogonal</strong> to {@link ValueType}: one widget can serve many value
 * types and vice-versa. For example {@code STRING} may be rendered as a plain input, a select, or a
 * file picker, while {@code FILE} always carries a file payload. UX-only nodes such as
 * {@code NOTICE} and {@code BUTTON} carry no value at all ({@code valueType} is {@code null}).
 */
public enum RenderType {
    STRING,
    NUMBER,
    DECIMAL,
    BOOLEAN,
    SELECT,
    MULTI_SELECT,
    GROUP,
    COLLECTION,
    FILE,
    DATE_TIME,
    HIDDEN,
    NOTICE,
    BUTTON
}
