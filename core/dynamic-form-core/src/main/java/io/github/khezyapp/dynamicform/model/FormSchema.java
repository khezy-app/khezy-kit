package io.github.khezyapp.dynamicform.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A versioned, immutable form definition — the form contract expressed as pure data (P1).
 * <p>
 * A schema owns a collection of field contracts plus an id/version pair for migrations (P10) and an
 * i18n title key (P14). It is Jackson-serializable by construction, so the same object served by the
 * backend is the contract the UI renders — no separate DTO.
 *
 * @param id       the stable schema identifier (e.g. {@code "kyc.personal-info"})
 * @param version  the schema version (P10)
 * @param titleKey the i18n key for the form title (P14)
 * @param fields   the field contracts in declaration order
 * @param meta     arbitrary extra data (e.g. {@code persistTo} hints for the workflow seam)
 */
public record FormSchema(
        String id,
        int version,
        String titleKey,
        List<FieldSchema> fields,
        Map<String, Object> meta
) {

    /**
     * Compact canonical constructor that normalises null facets.
     */
    public FormSchema {
        id = Objects.requireNonNull(id, "id must not be null");
        fields = Objects.nonNull(fields) ? List.copyOf(fields) : List.of();
        meta = Objects.nonNull(meta) ? Map.copyOf(meta) : Map.of();
    }

    /**
     * Creates a schema without metadata.
     *
     * @param id       the schema id
     * @param version  the schema version
     * @param titleKey the i18n title key
     * @param fields   the field contracts
     * @return a new schema
     */
    public static FormSchema of(final String id,
                                final int version,
                                final String titleKey,
                                final List<FieldSchema> fields) {
        return new FormSchema(id, version, titleKey, fields, null);
    }
}
