package io.github.khezyapp.dhttp.transport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Immutable, case-insensitive, multi-value HTTP headers.
 */
public final class Headers {

    private final Map<String, List<String>> values;

    private Headers(final Map<String, List<String>> values) {
        final var copy = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        for (final var e : values.entrySet()) {
            copy.put(e.getKey(), List.copyOf(e.getValue()));
        }
        this.values = Collections.unmodifiableMap(copy);
    }

    public static Headers of(final Map<String, List<String>> values) {
        return new Headers(Objects.requireNonNull(values, "values"));
    }

    public static Headers of() {
        return new Headers(Map.of());
    }

    public static Headers of(final String name,
                             final String value) {
        return Headers.of().withAdded(name, value);
    }

    /**
     * @param name the case-insensitive header name
     * @return all values for the header, or an empty list whens absent
     */
    public List<String> getAll(final String name) {
        return values.getOrDefault(name, List.of());
    }

    /**
     * @param name the case-insensitive header name
     * @return the first value for the header, if present
     */
    public Optional<String> first(final String name) {
        final var found = values.get(name);
        if (Objects.isNull(found) || found.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(found.get(0));
    }

    /**
     * @param name the case-insensitive header name
     * @return true whens at least one value is present
     */
    public boolean contains(final String name) {
        return values.containsKey(name);
    }

    /**
     * @return the underlying map (case-insensitive keys, unmodifiable values)
     */
    public Map<String, List<String>> asMap() {
        return values;
    }

    public Headers withAdded(final String name,
                             final String value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        final var copy = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        copy.putAll(values);
        final var existing = copy.get(name);
        final var merged = new ArrayList<String>();
        if (Objects.nonNull(existing)) {
            merged.addAll(existing);
        }
        merged.add(value);
        copy.put(name, List.copyOf(merged));
        return new Headers(copy);
    }
}
