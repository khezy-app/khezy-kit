package io.github.khezyapp.clone.strategy;

import io.github.khezyapp.clone.annotation.MarkAsImmute;
import io.github.khezyapp.clone.api.CloneContext;
import io.github.khezyapp.clone.api.CloneStrategy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.Temporal;
import java.util.Set;
import java.util.UUID;

/**
 * A performance-optimized strategy for immutable and semi-immutable types.
 * <p>
 * Instead of creating a new instance, this strategy returns the original reference.
 * It supports primitives, wrappers, Enums, Records, classes marked with {@link MarkAsImmute},
 * and internal Java immutable collection types.
 * </p>
 */
public class ImmutableStrategy implements CloneStrategy {
    private final Set<Class<?>> immutables = Set.of(
            String.class, Integer.class, Long.class, Short.class, Byte.class,
            Double.class, Float.class, Character.class, Boolean.class,
            BigDecimal.class, BigInteger.class, UUID.class
    );

    @Override
    public boolean support(final Class<?> clz) {
        // 1. Direct match for JDK standard types
        if (clz.isPrimitive() || immutables.contains(clz) || Temporal.class.isAssignableFrom(clz)) {
            return true;
        }

        // 2. Language features
        if (clz.isEnum() || clz.isRecord() || clz.isAnnotationPresent(MarkAsImmute.class)) {
            return true;
        }

        // 3. String-based detection for popular 3rd party libraries
        // This prevents ClassNotFoundException because we only check the String name
        final String name = clz.getName();
        return name.startsWith("java.time.") ||
                name.startsWith("org.joda.time.") ||
                name.startsWith("com.google.common.collect.Immutable") ||
                name.startsWith("java.util.ImmutableCollections") ||
                name.contains("Unmodifiable");
    }

    @Override
    public <T> T copy(final T origin,
                      final CloneContext context) {
        return origin;
    }
}
