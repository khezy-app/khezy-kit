package io.github.khezyapp.dynamicform.value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Immutable wrapper over the flat/nested form value map with dot-path and index addressing
 * (n8n's {@code parameterPath}).
 * <p>
 * Values are stored at top-level field names; nested {@code GROUP} objects are addressable as
 * {@code documents.idType} and {@code COLLECTION} rows as {@code directors[2].idNumber}. Every
 * mutation returns a new instance — instances are deeply immutable and safe to share.
 */
public final class FormValues {

    private static final Pattern INDEX_PATTERN = Pattern.compile("\\[(\\d+)\\]");

    private final Map<String, Object> values;

    @SuppressWarnings("unchecked")
    private FormValues(final Map<String, Object> values) {
        this.values = (Map<String, Object>) deepFreeze(values);
    }

    /**
     * Creates an empty value map.
     *
     * @return an empty instance
     */
    public static FormValues empty() {
        return new FormValues(Map.of());
    }

    /**
     * Wraps a map, defensively deep-copying it.
     *
     * @param values the raw values
     * @return an immutable view
     */
    public static FormValues of(final Map<String, Object> values) {
        return new FormValues(values);
    }

    /**
     * Reads a value by dot-path, returning {@code null} when any segment is absent.
     *
     * @param path the path (e.g. {@code "country"}, {@code "documents.idType"})
     * @return the value, or {@code null} if absent
     */
    public Object get(final String path) {
        return get(this.values, parse(path), 0);
    }

    /**
     * Whether a value exists at the path (even when the stored value is {@code null}).
     *
     * @param path the path
     * @return {@code true} when the leaf exists
     */
    public boolean has(final String path) {
        return has(this.values, parse(path), 0);
    }

    /**
     * Returns a new instance with the value set at the path.
     *
     * @param path  the path
     * @param value the value to store
     * @return a new instance
     */
    @SuppressWarnings("unchecked")
    public FormValues with(final String path,
                           final Object value) {
        final var updated = (Map<String, Object>) set(this.values, parse(path), 0, value);
        return new FormValues(updated);
    }

    /**
     * Returns a new instance with the value removed from the path.
     *
     * @param path the path of the value to drop
     * @return a new instance
     */
    @SuppressWarnings("unchecked")
    public FormValues without(final String path) {
        final var updated = (Map<String, Object>) drop(this.values, parse(path), 0);
        return new FormValues(updated);
    }

    /**
     * Returns a deeply immutable copy of the underlying map.
     *
     * @return an unmodifiable map
     */
    public Map<String, Object> asMap() {
        final var copy = new LinkedHashMap<String, Object>();
        for (final var entry : this.values.entrySet()) {
            copy.put(entry.getKey(), deepFreeze(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Returns the set of top-level field names present.
     *
     * @return an unmodifiable set
     */
    public Set<String> names() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(this.values.keySet()));
    }

    /**
     * Whether no values are stored.
     *
     * @return {@code true} when empty
     */
    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    private static Object get(final Object container,
                              final List<Object> steps,
                              final int index) {
        if (index >= steps.size()) {
            return container;
        }
        final var step = steps.get(index);
        if (step instanceof String key) {
            if (!(container instanceof Map<?, ?> map) || !map.containsKey(key)) {
                return null;
            }
            return get(map.get(key), steps, index + 1);
        }
        final var position = (Integer) step;
        if (!(container instanceof List<?> list) || position >= list.size()) {
            return null;
        }
        return get(list.get(position), steps, index + 1);
    }

    private static boolean has(final Object container, final List<Object> steps, final int index) {
        if (index >= steps.size()) {
            return true;
        }
        final var step = steps.get(index);
        if (step instanceof String key) {
            if (!(container instanceof Map<?, ?> map) || !map.containsKey(key)) {
                return false;
            }
            return has(map.get(key), steps, index + 1);
        }
        final var position = (Integer) step;
        if (!(container instanceof List<?> list) || position >= list.size()) {
            return false;
        }
        return has(list.get(position), steps, index + 1);
    }

    @SuppressWarnings("unchecked")
    private static Object set(final Object container,
                              final List<Object> steps,
                              final int index,
                              final Object value) {
        if (index >= steps.size()) {
            return value;
        }
        final var step = steps.get(index);
        if (step instanceof String key) {
            final Map<String, Object> source;
            if (container instanceof Map<?, ?> map) {
                source = (Map<String, Object>) map;
            } else {
                source = Map.of();
            }
            final var copy = new LinkedHashMap<>(source);
            final var child = source.get(key);
            copy.put(key, set(child, steps, index + 1, value));
            return copy;
        }
        final var position = (Integer) step;
        final List<Object> source;
        if (container instanceof List<?> list) {
            source = (List<Object>) list;
        } else {
            source = List.of();
        }
        final var copy = new ArrayList<>(source);
        while (copy.size() <= position) {
            copy.add(null);
        }
        final var child = position < source.size() ? source.get(position) : null;
        copy.set(position, set(child, steps, index + 1, value));
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static Object drop(final Object container,
                               final List<Object> steps,
                               final int index) {
        final var step = steps.get(index);
        if (step instanceof String key) {
            if (!(container instanceof Map<?, ?> map) || !map.containsKey(key)) {
                return container;
            }
            final var source = (Map<String, Object>) map;
            if (index == steps.size() - 1) {
                final var copy = new LinkedHashMap<>(source);
                copy.remove(key);
                return copy;
            }
            final var copy = new LinkedHashMap<>(source);
            copy.put(key, drop(source.get(key), steps, index + 1));
            return copy;
        }
        final var position = (Integer) step;
        if (!(container instanceof List<?> list) || position >= list.size()) {
            return container;
        }
        final var copy = new ArrayList<>((List<Object>) list);
        copy.set(position, drop(list.get(position), steps, index + 1));
        return copy;
    }

    private static Object deepFreeze(final Object value) {
        if (value instanceof Map<?, ?> map) {
            final var copy = new LinkedHashMap<String, Object>();
            for (final var entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), deepFreeze(entry.getValue()));
            }
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) {
            final var copy = new ArrayList<>();
            for (final var item : list) {
                copy.add(deepFreeze(item));
            }
            return Collections.unmodifiableList(copy);
        }
        return value;
    }

    private static List<Object> parse(final String path) {
        final var steps = new ArrayList<>();
        for (final var segment : path.split("\\.", -1)) {
            if (segment.isEmpty()) {
                continue;
            }
            final var bracketStart = segment.indexOf('[');
            if (bracketStart < 0) {
                steps.add(segment);
                continue;
            }
            if (bracketStart > 0) {
                steps.add(segment.substring(0, bracketStart));
            }
            final var matcher = INDEX_PATTERN.matcher(segment.substring(bracketStart));
            while (matcher.find()) {
                steps.add(Integer.valueOf(matcher.group(1)));
            }
        }
        return steps;
    }
}
