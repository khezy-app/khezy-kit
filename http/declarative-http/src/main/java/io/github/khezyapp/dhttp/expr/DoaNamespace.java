package io.github.khezyapp.dhttp.expr;

import io.github.khezyapp.doa.api.ObjectAccessor;

import java.util.Objects;

/**
 * Stateless bridge exposing the KHEZY dynamic-object library as the {@code doa} namespace inside
 * JEXL expressions, e.g. {@code doa.get($response, "data.items[0].id")}.
 */
public final class DoaNamespace {

    private final ObjectAccessor accessor;

    public DoaNamespace(final ObjectAccessor accessor) {
        this.accessor = Objects.requireNonNull(accessor, "accessor");
    }

    public Object get(final Object target,
                      final String path) {
        return accessor.get(target, path);
    }

    public Object set(final Object target,
                      final String path,
                      final Object value) {
        return accessor.set(target, path, value);
    }

    public Object remove(final Object target,
                         final String path) {
        return accessor.remove(target, path);
    }
}
