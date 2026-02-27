package io.github.khezyapp.doa.adapters;

import io.github.khezyapp.doa.api.TypeAdapter;
import io.github.khezyapp.doa.cache.Cache;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link TypeAdapter} that interacts with standard Java Beans using {@link MethodHandle}.
 * <p>
 * This adapter uses high-performance reflection to access properties via "get" and "set" methods.
 * It excludes Maps, Lists, and Records to avoid overlapping with specialized adapters.
 * </p>
 */
@Slf4j
public class BeanAdapter implements TypeAdapter {
    private final Cache<Map<String, MethodHandle>> getters;
    private final Cache<Map<String, MethodHandle>> setters;
    private final MethodHandles.Lookup lookup;

    /**
     * Constructs a new BeanAdapter with the provided caches for method handles.
     *
     * @param getters cache for getter method handles keyed by class name
     * @param setters cache for setter method handles keyed by class name
     */
    public BeanAdapter(final Cache<Map<String, MethodHandle>> getters,
                       final Cache<Map<String, MethodHandle>> setters) {
        this.getters = getters;
        this.setters = setters;
        this.lookup = MethodHandles.lookup();
    }

    /**
     * Determines if the target object is a standard POJO (not a Map, List, or Record).
     *
     * @param target the object to check
     * @return true if the object is supported by this adapter
     */
    @Override
    public boolean supports(final Object target) {
        return !isMap(target) &&
            !isList(target) &&
            !isRecord(target);
    }

    /**
     * Retrieves a property value from a Java Bean using a cached getter MethodHandle.
     *
     * @param target   the POJO instance
     * @param property the name of the property to retrieve
     * @return the value of the property, or null if the getter does not exist
     */
    @Override
    public Object getValue(final Object target,
                           final String property) {
        final var clz = target.getClass();
        final var clzGetters = getters.get(getClassKey(clz), key -> this.inspectGetters(clz));
        final var getter = clzGetters.get(property);
        if (Objects.isNull(getter)) {
            return null;
        }
        try {
            return getter.invoke(target);
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets a property value on a Java Bean using a cached setter MethodHandle.
     *
     * @param target   the POJO instance
     * @param property the name of the property to set
     * @param value    the value to assign
     * @return the target object
     */
    @Override
    public Object setValue(final Object target,
                           final String property,
                           final Object value) {
        final var clz = target.getClass();
        final var clzSetters = setters.get(getClassKey(clz), key -> this.inspectSetters(clz));
        final var setter = clzSetters.get(property);
        if (Objects.isNull(setter)) {
            return target;
        }
        try {
            setter.invoke(target, value);
            return target;
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isMap(final Object target) {
        return target instanceof Map;
    }

    private boolean isList(final Object target) {
        return target instanceof List;
    }

    private boolean isRecord(final Object target) {
        return target instanceof Record;
    }

    private String getClassKey(final Class<?> clz) {
        return clz.getName();
    }

    private Map<String, MethodHandle> inspectGetters(final Class<?> clz) {
        final var map = new HashMap<String, MethodHandle>();
        for (final var m : clz.getMethods()) {
            if (isJavaBeanGetter(m)) {
                final var prop = decap(m.getName().substring(3));
                try {
                    final var handleMethod = lookup.unreflect(m);
                    map.put(prop, handleMethod);
                } catch (final IllegalAccessException ignored) {
                    log.debug("Error make getter method of {}.{}", clz.getSimpleName(), m.getName());
                }
            }
        }
        return map;
    }

    private Map<String, MethodHandle> inspectSetters(final Class<?> clz) {
        final var map = new HashMap<String, MethodHandle>();
        for (final var m : clz.getMethods()) {
            if (isJavaBeanSetter(m)) {
                final var prop = decap(m.getName().substring(3));
                try {
                    final var handleMethod = lookup.unreflect(m);
                    map.put(prop, handleMethod);
                } catch (final IllegalAccessException ignored) {
                    log.debug("Error make setter method of {}.{}", clz.getSimpleName(), m.getName());
                }
            }
        }
        return map;
    }

    private boolean isJavaBeanGetter(final Method method) {
        return method.getParameterCount() == 0 &&
            method.getName().startsWith("get") &&
            method.getModifiers() != Modifier.PRIVATE &&
            !method.getName().equals("getClass");
    }

    private boolean isJavaBeanSetter(final Method method) {
        return method.getParameterCount() == 1 &&
            method.getName().startsWith("set") &&
            method.getModifiers() != Modifier.PRIVATE;
    }

    private String decap(final String property) {
        return Character.toLowerCase(property.charAt(0)) + property.substring(1);
    }
}
