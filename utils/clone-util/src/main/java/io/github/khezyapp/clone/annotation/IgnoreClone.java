package io.github.khezyapp.clone.annotation;

import java.lang.annotation.*;

/**
 * Annotation used to indicate that a specific class or field should be completely
 * skipped during the deep cloning process.
 * <p>
 * When applied to a {@link ElementType#TYPE}, any instance of that class will
 * return {@code null} when encountered. When applied to a {@link ElementType#FIELD},
 * that specific field will not be copied to the target object.
 * </p>
 */
@Documented
@Target(value = {ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreClone {
}
