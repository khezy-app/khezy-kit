package io.github.khezyapp.doa.adapters;

import io.github.khezyapp.doa.api.TypeAdapter;
import io.github.khezyapp.doa.cache.Cache;
import io.github.khezyapp.doa.cache.MapCache;

import java.util.List;

public class CompositeTypeAdapter implements TypeAdapter {
    private final List<TypeAdapter> adapters;
    private final Cache<TypeAdapter> adapterCache = new MapCache<>(250);

    public CompositeTypeAdapter(final List<TypeAdapter> adapters) {
        this.adapters = adapters;
    }

    @Override
    public boolean supports(final Object target) {
        return target != null;
    }

    @Override
    public Object getValue(final Object target,
                           final String property) {
        return getDelegate(target).getValue(target, property);
    }

    @Override
    public Object setValue(final Object target,
                           final String property,
                           final Object value) {
        return getDelegate(target).setValue(target, property, value);
    }

    private TypeAdapter getDelegate(final Object target) {
        final var clzName = target.getClass().getName();
        return adapterCache.get(clzName, clz ->
                adapters.stream()
                        .filter(a -> a.supports(target))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No adapter found for " + clz))
        );
    }
}
