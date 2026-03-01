package io.github.khezyapp.clone.annotation;

import java.lang.annotation.*;

/**
 * Annotation used to mark a class or field as immutable.
 * <p>
 * This informs the {@link io.github.khezyapp.clone.api.Cloner} that the object does not require a deep copy
 * and can be safely shared by reference. This is a performance optimization
 * for custom types that behave like {@link String} or {@link java.lang.Integer}.
 * </p>
 */
@Documented
@Target(value = {ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MarkAsImmute {
}
