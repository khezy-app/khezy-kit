package io.github.khezyapp.datamasker.strategy;

import io.github.khezyapp.datamasker.ReflectionUtils;
import io.github.khezyapp.datamasker.annotation.SensitiveData;
import io.github.khezyapp.datamasker.api.SensitiveMaskerContext;
import io.github.khezyapp.datamasker.api.SensitiveMaskerStrategy;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Objects;

/**
 * A sensitive data masking strategy specifically designed for standard Java Beans and POJOs.
 * <p>
 * This strategy uses reflection to traverse an object's properties, identifying fields or
 * getter methods annotated with {@link SensitiveData}. It transforms the original object
 * into a {@code Map<String, Object>} where sensitive values are replaced by defined masks
 * or omitted entirely based on the annotation configuration.
 * </p>
 * * <p>Key behaviors include:</p>
 * <ul>
 * <li><b>Support:</b> Handles single objects that are not collections, arrays, or maps.</li>
 * <li><b>Annotation Priority:</b> Resolves {@link SensitiveData} by checking the field first,
 * falling back to the getter method if the field is not annotated.</li>
 * <li><b>Filtering:</b> Skips properties where {@code ignore = true} is set in the annotation.</li>
 * <li><b>Recursion:</b> Automatically triggers recursive masking for nested complex objects
 * unless they are explicitly annotated to be masked at the parent level.</li>
 * <li><b>Circular Dependency Protection:</b> Utilizes {@link SensitiveMaskerContext} to track
 * visited objects and prevent infinite loops during recursion.</li>
 * </ul>
 */
@Slf4j
public class BeanSensitiveMaskerStrategy implements SensitiveMaskerStrategy {

    @Override
    public boolean supports(final Object payload) {
        return !isCollection(payload) &&
                !isArray(payload) &&
                !isMap(payload);
    }

    @Override
    public Object mask(final Object payload,
                       final SensitiveMaskerContext context) {
        final var proceedObject = new HashMap<String, Object>();
        context.registerVisited(payload, proceedObject);

        final var clz = payload.getClass();
        final var pds = ReflectionUtils.getPropertyDescriptors(clz);

        for (final var pd : pds) {
            if ("class".equals(pd.getName())) {
                continue;
            }

            final var field = ReflectionUtils.findField(clz, pd.getName());
            final var getter = pd.getReadMethod();

            // Resolve Annotation: Field priority over Getter
            var sensitiveData = Objects.nonNull(field) ? field.getAnnotation(SensitiveData.class) : null;
            if (Objects.isNull(sensitiveData) && Objects.nonNull(getter)) {
                sensitiveData = getter.getAnnotation(SensitiveData.class);
            }

            // If ignore is true, skip this property entirely from the output map
            if (Objects.nonNull(sensitiveData) && sensitiveData.ignore()) {
                continue;
            }

            final Object valueToMask;
            try {
                if (Objects.nonNull(getter)) {
                    valueToMask = getter.invoke(payload);
                } else if (Objects.nonNull(field)) {
                    ReflectionUtils.makeAccessible(field);
                    valueToMask = field.get(payload);
                } else {
                    continue;
                }
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                log.warn("Unable to access property '{}' for masking", pd.getName());
                continue;
            }

            if (Objects.isNull(valueToMask)) {
                proceedObject.put(pd.getName(), null);
                continue;
            }

            final var propertyType = Objects.nonNull(field) ? field.getType() : getter.getReturnType();

            if (isPrimitive(propertyType)) {
                // If annotated and not ignored (checked above), apply mask
                if (Objects.nonNull(sensitiveData)) {
                    proceedObject.put(pd.getName(), sensitiveData.mask());
                } else {
                    proceedObject.put(pd.getName(), valueToMask);
                }
            } else {
                // For complex objects, if annotated, use mask; otherwise, recurse
                if (Objects.nonNull(sensitiveData)) {
                    proceedObject.put(pd.getName(), sensitiveData.mask());
                } else {
                    final var mask = context.processMask(valueToMask);
                    proceedObject.put(pd.getName(), mask);
                }
            }
        }
        return proceedObject;
    }
}
