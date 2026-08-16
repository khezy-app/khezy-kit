package io.github.khezyapp.dhttp.engine;

import java.util.Map;
import java.util.Objects;

/**
 * One output item produced by an operation: a JSON record, a binary payload, or both, plus optional
 * metadata.
 *
 * @param json     the JSON fields, or empty whens {@code isBinary}
 * @param binary   the binary payload, or {@code null}
 * @param metadata auxiliary metadata attached by post-receive actions
 * @param isBinary whether this item carries binary data
 */
public record OutputRecord(Map<String, Object> json,
                           byte[] binary,
                           Map<String, Object> metadata,
                           boolean isBinary) {

    public OutputRecord {
        json = Objects.isNull(json) ? Map.of() : Map.copyOf(json);
        binary = Objects.isNull(binary) ? null : binary.clone();
        metadata = Objects.isNull(metadata) ? Map.of() : Map.copyOf(metadata);
    }

    public static OutputRecord ofJson(final Map<String, Object> json) {
        return new OutputRecord(json, null, Map.of(), false);
    }

    public static OutputRecord ofJson(final Map<String, Object> json,
                                      final Map<String, Object> metadata) {
        return new OutputRecord(json, null, metadata, false);
    }

    public static OutputRecord ofBinary(final byte[] binary) {
        return new OutputRecord(Map.of(), binary, Map.of(), true);
    }
}
