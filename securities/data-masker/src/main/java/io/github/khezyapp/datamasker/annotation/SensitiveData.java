package io.github.khezyapp.datamasker.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark fields containing sensitive information that should be masked or ignored
 * during serialization or logging processes.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface SensitiveData {

    /**
     * The replacement string used to mask the sensitive value.
     * @return the mask string, defaults to "***"
     */
    String mask() default "***";

    /**
     * If true, the field should be ignored entirely rather than masked.
     * @return true if the field should be skipped
     */
    boolean ignore() default false;
}
