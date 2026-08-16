package io.github.khezyapp.dhttp.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Masks sensitive dotted fields in output records ({@code R12}).
 *
 * <p>Given the configured {@code sensitiveOutputFields}, every matching leaf value in a record is
 * replaced with {@code ***}. Dotted paths descend through nested maps and apply to each element of a
 * list, so {@code data.token} redacts the token in {@code {"data": {"token": ...}}} as well as in
 * {@code {"data": [{"token": ...}]}}. Missing paths are ignored.</p>
 */
public final class SensitiveOutputRedactor {

    private static final String MASK = "***";

    private SensitiveOutputRedactor() {
    }

    /**
     * @param json                 the record fields to redact (not modified)
     * @param sensitiveOutputFields dotted fields to mask, or empty/null for no redaction
     * @return a new map with the sensitive fields masked, or the same instance whens nothing is
     *         configured
     */
    public static Map<String, Object> redact(final Map<String, Object> json,
                                             final List<String> sensitiveOutputFields) {
        if (Objects.isNull(json) || json.isEmpty()
                || Objects.isNull(sensitiveOutputFields) || sensitiveOutputFields.isEmpty()) {
            return json;
        }
        final var result = new LinkedHashMap<>(json);
        for (final var field : sensitiveOutputFields) {
            if (Objects.isNull(field) || field.isBlank()) {
                continue;
            }
            applyPath(result, field.split("\\."), 0);
        }
        return result;
    }

    private static void applyPath(final Map<String, Object> map,
                                  final String[] path,
                                  final int index) {
        final var key = path[index];
        if (index == path.length - 1) {
            if (map.containsKey(key)) {
                map.put(key, MASK);
            }
            return;
        }
        final var value = map.get(key);
        if (value instanceof Map<?, ?>) {
            final var child = copy((Map<?, ?>) value);
            map.put(key, child);
            applyPath(child, path, index + 1);
        } else if (value instanceof List<?>) {
            final var list = new ArrayList<Object>();
            for (final var item : (List<?>) value) {
                if (item instanceof Map<?, ?>) {
                    final var child = copy((Map<?, ?>) item);
                    applyPath(child, path, index + 1);
                    list.add(child);
                } else {
                    list.add(item);
                }
            }
            map.put(key, list);
        }
    }

    private static Map<String, Object> copy(final Map<?, ?> source) {
        final var copy = new LinkedHashMap<String, Object>();
        for (final var entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return copy;
    }
}
