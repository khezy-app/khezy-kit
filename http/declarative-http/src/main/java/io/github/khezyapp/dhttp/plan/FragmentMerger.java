package io.github.khezyapp.dhttp.plan;

import io.github.khezyapp.dhttp.spec.HttpRequestSpec;
import io.github.khezyapp.dhttp.spec.Operation;
import io.github.khezyapp.dhttp.spec.RequestShape;
import io.github.khezyapp.dhttp.spec.Route;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Deep-merges spec-level defaults under operation/route-level overrides ({@code R2}).
 *
 * <p>Merging is per-key: a value present in the route wins over the spec default for that key;
 * nested maps are merged recursively rather than replaced wholesale.</p>
 */
public final class FragmentMerger {

    private FragmentMerger() {
    }

    /**
     * Merges spec defaults ({@code defaultHeaders}, {@code defaultPagination}) under the route's
     * request, producing a new {@link Route}.
     *
     * @param spec      the root spec providing defaults
     * @param operation the operation being planned (kept for API stability)
     * @param route     the operation's route
     * @return the merged route
     */
    public static Route mergeDefaults(final HttpRequestSpec spec,
                                      final Operation operation,
                                      final Route route) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(route, "route");
        final var shape = route.request();
        final var request = new RequestShape(
                shape.method(),
                shape.path(),
                mergeHeaders(spec.defaultHeaders(), shape.headers()),
                shape.query(),
                shape.json(),
                shape.encoding(),
                shape.baseUrl());
        final var pagination = Objects.nonNull(route.pagination()) ? route.pagination() : spec.defaultPagination();
        return new Route(request, route.sends(), route.output(), pagination, route.preSends());
    }

    /**
     * Recursively merges {@code overrides} on top of {@code base}; nested maps merge by key.
     *
     * @param base      the base map
     * @param overrides the overriding entries
     * @return a new merged map (originals are not modified)
     */
    public static Map<String, Object> deepMerge(final Map<String, Object> base,
                                                final Map<String, Object> overrides) {
        final var result = new LinkedHashMap<>(base);
        for (final var entry : overrides.entrySet()) {
            final var existing = result.get(entry.getKey());
            if (existing instanceof Map && entry.getValue() instanceof Map) {
                result.put(entry.getKey(), deepMerge(cast(existing), cast(entry.getValue())));
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(final Object value) {
        return (Map<String, Object>) value;
    }

    private static Map<String, String> mergeHeaders(final Map<String, String> defaults,
                                                    final Map<String, String> overrides) {
        final var result = new LinkedHashMap<>(defaults);
        result.putAll(overrides);
        return result;
    }
}
